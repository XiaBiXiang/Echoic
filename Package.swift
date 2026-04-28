// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "Echoic",
    platforms: [.macOS(.v14)],
    products: [
        .executable(name: "Echoic", targets: ["Echoic"]),
    ],
    targets: [
        .executableTarget(
            name: "Echoic",
            path: "Echoic"
        ),
    ]
)
