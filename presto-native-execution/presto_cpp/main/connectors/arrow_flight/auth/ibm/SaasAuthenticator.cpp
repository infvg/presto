#include "presto_cpp/main/connectors/arrow_flight/auth/ibm/SaasAuthenticator.h"
#include <arrow/flight/api.h>
#include <curl/curl.h>
#include "presto_cpp/external/json/nlohmann/json.hpp"
#include "presto_cpp/main/http/HttpConstants.h"

namespace facebook::presto::ibm {

namespace {

static size_t
writeFunction(char* data, size_t size, size_t nmemb, void* userdata) {
  std::string* response = static_cast<std::string*>(userdata);
  response->append(data, size * nmemb);
  return size * nmemb;
}

} // end namespace

using json = nlohmann::json;

void SaasAuthenticator::authenticateClient(
    std::unique_ptr<arrow::flight::FlightClient>& client,
    const velox::config::ConfigBase* sessionProperties,
    const std::map<std::string, std::string>& extraCredentials,
    arrow::flight::AddCallHeaders& headerWriter) {
  headerWriter.AddHeader("authorization", "Bearer " + getToken());
}

std::string SaasAuthenticator::getToken() {
  auto tokenExpiryDuration =
      std::chrono::seconds(saasAuthenticatorConfig_->tokenExpiryTimeSec());
  std::lock_guard lock(tokenMutex_);
  auto now = std::chrono::system_clock::now();
  auto elapsed = now - tokenGeneratedTime_;
  if (!currentToken_.hasValue() || elapsed >= tokenExpiryDuration) {
    auto tokenUrl = saasAuthenticatorConfig_->tokenUrl();
    auto apiKey = saasAuthenticatorConfig_->apiKey();
    VELOX_CHECK(tokenUrl, "Arrow flight server token url not given");
    VELOX_CHECK(apiKey, "Arrow flight server api key not given");
    currentToken_ = fetchToken(apiKey.value(), tokenUrl.value());
    tokenGeneratedTime_ = now;
  }
  return currentToken_.value();
}

std::string SaasAuthenticator::fetchToken(
    std::string_view apiKey,
    std::string_view tokenUrl) {
  CURL* curl = curl_easy_init();
  VELOX_CHECK_NOT_NULL(curl, "Failed to initialize libcurl");

  // Prepare curl headers
  struct curl_slist* headers = nullptr;
  headers = curl_slist_append(
      headers, "Content-Type: application/x-www-form-urlencoded");
  headers = curl_slist_append(headers, "Accept: application/json");

  std::string url = std::string(tokenUrl);
  std::string body =
      "response_type=cloud_iam&grant_type=urn:ibm:params:oauth:grant-type:apikey&apikey=" +
      std::string(apiKey);

  curl_easy_setopt(curl, CURLOPT_URL, url.c_str());

  curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
  curl_easy_setopt(curl, CURLOPT_POST, 1L);
  curl_easy_setopt(curl, CURLOPT_POSTFIELDS, body.c_str());
  curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, body.size());
  curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeFunction);
  curl_easy_setopt(curl, CURLOPT_FAILONERROR, 1);
  curl_easy_setopt(curl, CURLOPT_SSLVERSION, CURL_SSLVERSION_TLSv1_2);

  std::string response;
  curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);

  // Perform the request
  long httpCode{0};
  CURLcode res = curl_easy_perform(curl);
  curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpCode);
  if (httpCode != http::kHttpOk) {
    VELOX_FAIL(
        "Arrow flight server token fetching failed. CurlError: {}, HttpCode: {}",
        curl_easy_strerror(res),
        httpCode);
  }

  // Cleanup
  curl_slist_free_all(headers);
  curl_easy_cleanup(curl);

  return json::parse(response).at("access_token").get<std::string>();
}

AFC_REGISTER_AUTH_FACTORY(std::make_shared<SaasAuthenticatorFactory>())

} // namespace facebook::presto::ibm
