import XCTest
@testable import PrivacyGuard

final class PrivacyGuardTests: XCTestCase {
    @MainActor
    func testPrivacyGuardManagerExists() throws {
        // Verify the main manager is accessible and functional
        let manager = PrivacyGuardManager.shared
        manager.startMonitoring()
        manager.stopMonitoring()
    }
    
    @MainActor
    func testExample() throws {
        // Placeholder test exercising main actor context
    }
}