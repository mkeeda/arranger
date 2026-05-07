# Contributing to Arranger

Thank you for your interest in contributing to Arranger! This document outlines our development process, versioning strategy, and release workflow.

## Reporting Issues

If you find a bug or have a feature request, please open an issue on GitHub.
To help us resolve issues quickly, please include the following information:

**For Bug Reports:**
* **Expected behavior**: What did you expect to happen?
* **Actual behavior**: What actually happened? (Include screenshots or GIFs if it's a UI issue).
* **Steps to reproduce**: A clear and concise list of steps to reproduce the issue.
* **Environment**: Your Android OS version, Compose version, and device/emulator details.

**For Feature Requests:**
* **Use case**: Why do you need this feature? What problem does it solve?
* **Proposed API/Solution**: If you have an idea of how the API should look, please share it!

If you'd like to work on an issue yourself, please comment on it first to let us know, so we can avoid duplicate effort.

## Versioning Strategy (0.x.x Phase)

Arranger follows [Semantic Versioning](https://semver.org/), but while we are in the `0.x.x` pre-v1.0 phase, we apply the following specific rules to set clear expectations:

* **Minor Version (`0.X.0`)**: Incremented when we enter a new development phase (milestone) or introduce significant architectural/breaking changes.
* **Patch Version (`0.x.Y`)**: Generally not used during the pre-release phase. We rely on pre-release suffixes instead.
* **Pre-release Suffix (`-alphaXX`, `-betaXX`)**: Incremented for smaller features, bug fixes, or minor API tweaks within the same milestone (e.g., `0.1.0-alpha01` -> `0.1.0-alpha02`).
    * `-alpha`: Features are still incomplete or APIs are subject to rapid change.
    * `-beta`: The feature set for the milestone is mostly complete, focusing on stabilization and bug fixes.

*Note: The single source of truth for the project's current version is the `VERSION_NAME` property in the root `gradle.properties` file.*

## Release Process (Tag-Driven)

We use an automated CI/CD pipeline triggered by a tag push matching `v*`. If you are a maintainer, follow these steps to publish a new version:

1.  **Update Version**: Update the `VERSION_NAME` in `gradle.properties` (e.g., to `0.1.0-alpha01`) and merge it into the `main` branch.
2.  **Draft Release**: Go to GitHub Releases and click "Draft a new release".
3.  **Create Tag**: Create a new tag matching the version with a `v` prefix (e.g., `v0.1.0-alpha01`).
4.  **Generate Notes**: Use the "Generate release notes" button to auto-generate the changelog based on merged PRs.
5.  **Publish**: Click "Publish release". GitHub will push the associated tag, which automatically triggers the GitHub Actions workflow to build, sign, and publish the artifacts to Maven Central.

*Note: Because the workflow listens to tag pushes, pushing a `v*` tag directly via git (without creating a GitHub Release) will also trigger the publish workflow. Please be careful when managing tags locally.*

## Commit Guidelines

This project strictly follows the [Conventional Commits](https://www.conventionalcommits.org/) specification. Please ensure your commit messages and PR titles adhere to this format (e.g., `feat(editor): add text insertion API`, `fix(core): resolve span calculation overlap`).
