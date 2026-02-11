#!/bin/bash
# Setup script for ShopKeeper Pro git hooks

echo "🔧 Setting up git hooks for ShopKeeper Pro..."

# Configure git to use the .githooks directory
git config core.hooksPath .githooks

if [ $? -eq 0 ]; then
    echo "✅ Git hooks configured successfully!"
    echo "📍 Hooks location: .githooks/"
    echo ""
    echo "Available hooks:"
    echo "  - pre-commit: Runs tests and checks coverage before commits"
    echo ""
    echo "To bypass hooks temporarily (not recommended):"
    echo "  git commit --no-verify"
else
    echo "❌ Failed to configure git hooks"
    exit 1
fi