import SwiftUI
import UIKit
import Shared

struct CountersStackView<Content: View>: UIViewControllerRepresentable {
    var components: [CounterComponent]
    var renderBody: (CounterComponent) -> Content

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeUIViewController(context: Context) -> UINavigationController {
        context.coordinator.syncChanges(self)
        let navigationController = UINavigationController(
                rootViewController: context.coordinator.viewControllers.first!)

        return navigationController
    }

    func updateUIViewController(_ navigationController: UINavigationController, context: Context) {
        context.coordinator.syncChanges(self)
        navigationController.setViewControllers(context.coordinator.viewControllers, animated: true)
    }

    private func createViewController(_ component: CounterComponent, _ coordinator: Coordinator) -> NavigationItemHostingController<Content> {
        let controller = NavigationItemHostingController(rootView: renderBody(component))
        controller.coordinator = coordinator
        controller.component = component
        controller.navigationItem.title = component.model.value.title
        return controller
    }

    class Coordinator: NSObject {
        var parent: CountersStackView<Content>
        var viewControllers = [NavigationItemHostingController<Content>]()
        var preservedComponents = [CounterComponent]()

        init(_ parent: CountersStackView<Content>) {
            self.parent = parent
        }

        func syncChanges(_ parent: CountersStackView<Content>) {
            self.parent = parent
            let count = max(preservedComponents.count, parent.components.count)

            for i in 0..<count {
                if (i >= parent.components.count) {
                    viewControllers.removeLast()
                } else if (i >= preservedComponents.count) {
                    viewControllers.append(parent.createViewController(parent.components[i], self))
                } else if (parent.components[i] !== preservedComponents[i]) {
                    viewControllers[i] = parent.createViewController(parent.components[i], self)
                }
            }

            preservedComponents = parent.components
        }
    }

    class NavigationItemHostingController<Content: View>: UIHostingController<Content> {
        fileprivate(set) weak var coordinator: Coordinator?
        fileprivate(set) weak var component: CounterComponent?

        override func viewDidDisappear(_ animated: Bool) {
            super.viewDidDisappear(animated)

            if isMovingFromParent && coordinator?.preservedComponents.last === component {
                component?.onPrevClicked()
            }
        }
    }
}

// stubs for XCode < 14:
#if compiler(<5.7)
struct NavigationStack<Path, Root>: View {
    var path: Path
    @ViewBuilder var root: () -> Root
    var body: some View {
        EmptyView()
    }
}

extension View {
    public func navigationDestination<D, C>(for data: D.Type, @ViewBuilder destination: @escaping (D) -> C) -> some View where D: Hashable, C: View {
        EmptyView()
    }
}
#endif
