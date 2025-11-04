#pragma once

#include <string>
#include <unordered_map>

#include <filesystem>
namespace fs = std::filesystem;

namespace facebook::presto {

struct CryptUtils {
  CryptUtils() = default;

  static constexpr int kBufferSize = 2 * 4096;

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
