require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))
folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -Wno-comma -Wno-shorten-64-to-32'

hyper_sdk_version = "2.2.1.4"

begin
  package_json_path = File.expand_path(File.join(__dir__, "../../package.json"))
  apps_package = JSON.parse(File.read(package_json_path))
  if apps_package["hyperSdkIOSVersion"]
    override_version = apps_package["hyperSdkIOSVersion"]
    hyper_sdk_version = Gem::Version.new(override_version) > Gem::Version.new(hyper_sdk_version) ? override_version : hyper_sdk_version
    if hyper_sdk_version != override_version
      puts ("Ignoring the overriden SDK version present in package.json (#{override_version}) as there is a newer version present in the SDK (#{hyper_sdk_version}).").yellow
    end
  end
rescue => e
  puts ("An error occurred while overrding the IOS SDK Version. #{e.message}").red
end

puts ("HyperSDK Version: #{hyper_sdk_version}")

Pod::Spec.new do |s|
  s.name         = "hyper-sdk-react"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "12.0" }
  s.source       = { :git => "https://github.com/juspay/hyper-sdk-react.git", :tag => "v#{s.version}" }

  s.static_framework = true
  s.source_files = "ios/**/*.{h,m,mm,swift,cpp}"

  if respond_to?(:install_modules_dependencies, true)
    install_modules_dependencies(s)
  else
    s.dependency "React-Core"

    # Don't install the dependencies when we run `pod install` in the old architecture.
    if ENV['RCT_NEW_ARCH_ENABLED'] == '1' then
      s.compiler_flags = folly_compiler_flags + " -DRCT_NEW_ARCH_ENABLED=1"
      s.pod_target_xcconfig    = {
          "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\"",
          "OTHER_CPLUSPLUSFLAGS" => "-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1",
          "CLANG_CXX_LANGUAGE_STANDARD" => "c++17"
      }
      s.dependency "React-RCTFabric"
      s.dependency "React-Codegen"
      s.dependency "RCT-Folly"
      s.dependency "RCTRequired"
      s.dependency "RCTTypeSafety"
      s.dependency "ReactCommon/turbomodule/core"
      s.dependency "HyperSDK", hyper_sdk_version
    end
  end
  s.dependency "HyperSDK", hyper_sdk_version
end
