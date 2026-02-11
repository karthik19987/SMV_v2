#!/bin/bash
# Comprehensive test runner for ShopKeeper Pro

echo "🧪 ShopKeeper Pro Test Suite"
echo "============================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Run unit tests first (Priority 1 - User preference)
echo ""
echo "1️⃣  Running Unit Tests..."
echo "------------------------"
./gradlew clean test

unit_result=$?
if [ $unit_result -ne 0 ]; then
    echo -e "${RED}❌ Unit tests failed!${NC}"
else
    echo -e "${GREEN}✅ Unit tests passed!${NC}"
fi

# Generate coverage report
echo ""
echo "2️⃣  Generating Coverage Report..."
echo "--------------------------------"
./gradlew jacocoTestReport

# Display coverage if report exists
if [ -f "app/build/reports/jacoco/test/jacocoTestReport.xml" ]; then
    covered=$(grep -oE 'type="LINE"[^>]*covered="[0-9]+"' app/build/reports/jacoco/test/jacocoTestReport.xml | head -1 | grep -oE 'covered="[0-9]+"' | grep -oE '[0-9]+')
    missed=$(grep -oE 'type="LINE"[^>]*missed="[0-9]+"' app/build/reports/jacoco/test/jacocoTestReport.xml | head -1 | grep -oE 'missed="[0-9]+"' | grep -oE '[0-9]+')

    if [ -n "$covered" ] && [ -n "$missed" ]; then
        total=$((covered + missed))
        if [ $total -gt 0 ]; then
            percent=$((covered * 100 / total))
            echo -e "📊 Test Coverage: ${GREEN}$percent%${NC}"

            if [ $percent -lt 80 ]; then
                echo -e "${YELLOW}⚠️  Warning: Coverage below 80% threshold${NC}"
            fi
        fi
    fi

    echo "📄 Detailed coverage report: app/build/reports/jacoco/test/html/index.html"
fi

# Run instrumentation tests if requested
if [ "$1" == "--with-android" ] || [ "$1" == "-a" ]; then
    echo ""
    echo "3️⃣  Running Android Instrumentation Tests..."
    echo "------------------------------------------"
    echo -e "${YELLOW}Note: This requires an emulator or connected device${NC}"

    ./gradlew connectedAndroidTest
    android_result=$?

    if [ $android_result -ne 0 ]; then
        echo -e "${RED}❌ Android tests failed!${NC}"
    else
        echo -e "${GREEN}✅ Android tests passed!${NC}"
    fi
fi

# Summary
echo ""
echo "📊 Test Summary"
echo "==============="

if [ $unit_result -eq 0 ]; then
    echo -e "Unit Tests: ${GREEN}PASSED${NC}"
else
    echo -e "Unit Tests: ${RED}FAILED${NC}"
fi

if [ -n "$percent" ]; then
    if [ $percent -ge 80 ]; then
        echo -e "Coverage: ${GREEN}$percent%${NC} ✅"
    else
        echo -e "Coverage: ${YELLOW}$percent%${NC} (minimum 80% required)"
    fi
fi

if [ "$1" == "--with-android" ] || [ "$1" == "-a" ]; then
    if [ $android_result -eq 0 ]; then
        echo -e "Android Tests: ${GREEN}PASSED${NC}"
    else
        echo -e "Android Tests: ${RED}FAILED${NC}"
    fi
fi

echo ""
echo "Usage:"
echo "  ./test.sh              # Run unit tests only (fast)"
echo "  ./test.sh --with-android   # Run all tests including Android tests"
echo "  ./test.sh -a           # Short form for --with-android"