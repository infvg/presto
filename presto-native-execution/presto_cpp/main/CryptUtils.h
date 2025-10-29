#pragma once

#include <string>
#include <unordered_map>

#if __has_include(<filesystem>)
#include <filesystem>
namespace fs = std::filesystem;
#else
#include <experimental/filesystem>
namespace fs = std::experimental::filesystem;
#endif

namespace facebook::presto {

struct CryptUtils {
  CryptUtils() = default;

 private:
  static std::unordered_map<std::string, std::string> loadProperties(
      const std::string& filePath);
  static std::unordered_map<std::string, std::string> decryptProperties(
      const std::unordered_map<std::string, std::string>& encProps);

 public:
  // Load decrypted properties from a file.
  // Returns an empty map if the file does not exist or cannot be read.
  static std::unordered_map<std::string, std::string> loadDecryptedProperties();
};

} // namespace facebook::presto
