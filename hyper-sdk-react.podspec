require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

rn_minor_version = 0
  [
  '../react-native/package.json',
  '../../react-native/package.json',
  '../../../react-native/package.json',
  ].each do |relative_path|
  if rn_minor_version == 0
    path = File.join(__dir__, relative_path)
    if File.exist?(path)
      begin
        package1  = JSON.parse(File.read(path))
        version = package1 ['version']
        if version == '*' || version.include?('*')
          rn_minor_version = 80
        else
          rn_minor_version = version.split('.')[1].to_i
        end
        break
      rescue => e
      end
    end
  end
  end

#Fallback - search in common locations
if rn_minor_version == 0
 common_paths = [
   File.expand_path('node_modules/react-native/package.json', Dir.pwd),
   File.expand_path('../../node_modules/react-native/package.json', __dir__),
 ]
  common_paths.each do |path|
   if File.exist?(path)
     begin
       package1  = JSON.parse(File.read(path))
       version = package1 ['version']
       if version == '*' || version.include?('*')
         rn_minor_version = 80
       else
         rn_minor_version = version.split('.')[1].to_i
       end
       break
     rescue => e
     end
   end
 end
end

# Fallback if still not found
if rn_minor_version == 0
  rn_minor_version = 77
end
puts ("Found react native minor version as #{rn_minor_version}").yellow

folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -Wno-comma -Wno-shorten-64-to-32'

hyper_sdk_version = "2.2.2.8"

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

# Prepare source files based on RN version
source_files_array = ["ios/**/*.{h,m,mm,swift}"]
exclude_files = []

if rn_minor_version >= 78
 source_files_array << "ios/latest/**/*.{h,m,mm,swift}"
 exclude_files << "ios/rn77/**/*"
else
 source_files_array << "ios/rn77/**/*.{h,m,mm,swift}"
 exclude_files << "ios/latest/**/*"
end

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
 s.source_files = source_files_array

 # Set exclude files
 s.exclude_files = exclude_files

 s.dependency "React-Core"
 s.dependency "React-RCTAppDelegate"
 s.dependency "HyperSDK", hyper_sdk_version
end
