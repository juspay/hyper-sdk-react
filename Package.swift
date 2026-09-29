// swift-tools-version: 6.0
import PackageDescription

// Rewritten in place by scripts/write-hyper-sdk-version.js on `npm install`
// (reads hyperSdkIOSVersion from the consuming app's package.json).
var hyperSdkVersion: Version = "2.2.3"

let package = Package(
    name: "HyperSdkReact",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "HyperSdkReact", targets: ["HyperSdkReact"]),
    ],
    dependencies: [
        .package(name: "ReactNative", path: "../../../../xcframeworks"),
        .package(name: "React-GeneratedCode", path: "../../../ios"),
        .package(url: "https://github.com/juspay/hypersdk-ios.git", exact: hyperSdkVersion),
    ],
    targets: [
        .target(
            name: "HyperSdkReact",
            dependencies: [
                .product(name: "ReactHeaders", package: "ReactNative"),
                .product(name: "ReactNativeHeaders", package: "ReactNative"),
                .product(name: "ReactNativeDependenciesHeaders", package: "ReactNative"),
                .product(name: "ReactAppHeaders", package: "React-GeneratedCode"),
                .product(name: "HyperSDK", package: "hypersdk-ios"),
            ],
            path: ".",
            exclude: ["example", "node_modules"],
            sources: [
                "ios/include/HyperMerchantView.h",
                "ios/include/HyperSdkReact.h",
                "ios/HyperSdkReact.mm",
                "ios/latest/HyperMerchantView.mm",
            ],
            publicHeadersPath: "ios/include"
        ),
    ],
    cxxLanguageStandard: .cxx20
)
