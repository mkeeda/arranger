// swift-tools-version: 5.9
import PackageDescription
import AppleProductTypes

let package = Package(
    name: "ArrangerSample",
    platforms: [.iOS(.v16)],
    products: [
        .iOSApplication(
            name: "ArrangerSample",
            targets: ["ArrangerSample"],
            displayVersion: "1.0",
            bundleVersion: "1",
            supportedDeviceFamilies: [.pad, .phone],
            supportedInterfaceOrientations: [.portrait, .landscapeRight, .landscapeLeft]
        )
    ],
    targets: [
        .executableTarget(
            name: "ArrangerSample",
            dependencies: ["shared"],
            path: "Sources/ArrangerSample"
        ),
        .binaryTarget(
            name: "shared",
            path: "../../shared/build/XCFrameworks/debug/shared.xcframework"
        )
    ]
)
