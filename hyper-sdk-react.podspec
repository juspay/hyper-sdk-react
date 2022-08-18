require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

hyper_sdk_version = "2.1.15"

begin
  apps_package = JSON.parse(File.read(File.join(__dir__, "../../package.json")))
  if apps_package["hyperSdkIOSVersion"]
    override_version = apps_package["hyperSdkIOSVersion"]
    hyper_sdk_version = Gem::Version.new(override_version) > Gem::Version.new(hyper_sdk_version) ? override_version : hyper_sdk_version
    if hyper_sdk_version != override_version
      puts ("Ignoring the overriden SDK version present in package.json (#{override_version}) as there is a newer version present in the SDK (#{hyper_sdk_version}).")
    end
  end
end

puts ("HyperSDK Version: #{hyper_sdk_version}")

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
