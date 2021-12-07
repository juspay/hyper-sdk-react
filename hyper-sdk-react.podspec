require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

begin
  apps_package = JSON.parse(File.read(File.join(__dir__, "../../package.json")))
  if apps_package["hyperSdkIOSVersion"]
    hyper_sdk_version = apps_package["hyperSdkIOSVersion"]
  else
    hyper_sdk_version = "2.1.0"
  end
rescue
  hyper_sdk_version = "2.1.0"
end

Pod::Spec.new do |s|
  s.name         = "hyper-sdk-react"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "10.0" }
  s.source       = { :git => "https://bitbucket.org/juspay/hyper-sdk-react.git", :tag => "v#{s.version}" }

  s.static_framework = true
  s.source_files = "ios/**/*.{h,m,mm,swift}"


  s.dependency "React"
  s.dependency "HyperSDK", hyper_sdk_version
end
