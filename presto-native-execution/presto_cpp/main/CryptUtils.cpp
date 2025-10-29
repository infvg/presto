#include "CryptUtils.h"

#include <dlfcn.h>
#include <glog/logging.h>
#include <fstream>
#include <iostream>
#include <sstream>
#include <stdexcept>
#include <vector>

namespace facebook::presto {

constexpr char const* kLHSecretPropsFileEnv = "IBMLH_SECRET_PROPS_FILE";
constexpr char const* kLHDefaultSecretPropsFile =
    "/mnt/infra/ibm-lh-secrets/preload_secrets.env";

// Load .properties file into a map
std::unordered_map<std::string, std::string> CryptUtils::loadProperties(
    const std::string& filePath) {
  std::unordered_map<std::string, std::string> properties;
  std::ifstream file(filePath);
  if (!file.is_open()) {
    throw std::runtime_error("Unable to open file: " + filePath);
  }

  std::string line;
  while (std::getline(file, line)) {
    if (line.empty() || line[0] == '#')
      continue;

    auto delimiterPos = line.find('=');
    if (delimiterPos == std::string::npos)
      continue;

    std::string key = line.substr(0, delimiterPos);
    std::string value = line.substr(delimiterPos + 1);
    properties[key] = value;
  }

  return properties;
}

std::unordered_map<std::string, std::string> CryptUtils::decryptProperties(
    const std::unordered_map<std::string, std::string>& encProps) {
  std::unordered_map<std::string, std::string> props;
  constexpr int OUT_LEN = 2 * 4096;

  using decrypt_func_t = int (*)(char*, char*);

  static decrypt_func_t do_decrypt_string_fn = nullptr;

  if (!do_decrypt_string_fn) {
    void* sym = dlsym(RTLD_DEFAULT, "do_decrypt_string");
    if (!sym) {
      throw std::runtime_error(
          "Missing symbol do_decrypt_string — ensure LD_PRELOAD contains CryptUtils");
    }
    do_decrypt_string_fn = reinterpret_cast<decrypt_func_t>(sym);
  }

  for (const auto& [key, encValue] : encProps) {
    std::vector<char> buffer(OUT_LEN, 0);

    int decLen = do_decrypt_string_fn(
        const_cast<char*>(encValue.c_str()), buffer.data());

    if (decLen < 0 || decLen > OUT_LEN) {
      throw std::runtime_error("Decryption failed for key: " + key);
    }

    props[key] = std::string(buffer.data(), decLen);
  }

  return props;
}

std::unordered_map<std::string, std::string>
CryptUtils::loadDecryptedProperties() {
  try {
    const char* secretFilePath = std::getenv(kLHSecretPropsFileEnv);
    if (!secretFilePath) {
      LOG(INFO) << "Mising LH secrets file env variable. Using default path";
      secretFilePath = kLHDefaultSecretPropsFile;
    }
    auto encProps = loadProperties(secretFilePath);
    return decryptProperties(encProps);
  } catch (const std::exception& e) {
    LOG(WARNING) << "Error loading decrypted properties: " << e.what();
    return {};
  }
}
} // namespace facebook::presto
