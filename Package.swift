// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "PrivacyGuard",
    platforms: [.iOS(.v18)],
    products: [
        .library(
            name: "PrivacyGuard",
            targets: ["PrivacyGuard"]
        ),
        .executable(
            name: "PrivacyGuardDemo",
            targets: ["PrivacyGuardDemo"]
        )
    ],
    targets: [
        .target(
            name: "PrivacyGuard",
            dependencies: [],
            path: "ProofOfConcept",
            exclude: ["README.md"]
        ),
        .executableTarget(
            name: "PrivacyGuardDemo",
            dependencies: ["PrivacyGuard"],
            path: "Demo"
        ),
        .testTarget(
            name: "PrivacyGuardTests",
            dependencies: ["PrivacyGuard"],
            path: "Tests/PrivacyGuardTests"
        )
    ]
)