echo "🔧 Compiling Java files..."
javac -cp "lib/*" src/*.java

# AI environment variables (override these in your shell or keep defaults)
export OPENROUTER_API_URL="https://openrouter.ai/api/v1/chat/completions"
export YOUR_API_KEY="YOUR_API_KEY"
export OPENROUTER_MODEL="google/gemma-3-27b-it:free"

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "🚀 Starting Library Management System..."
    echo "========================================="
    java -cp "src:lib/*" LibraryGUI
else
    echo "❌ Compilation failed!"
    echo "Check:"
    echo "1. Is mysql-connector-j-9.5.0.jar in lib/?"
    echo "2. Are all Java files in src/?"
    echo "3. Is Java installed? (java -version)"
fi
