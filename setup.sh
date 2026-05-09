echo "📚 Setting up Library Management System..."
cat > database/library_system.sql << 'SQL_EOF'
-- Paste ALL your SQL code from earlier here
-- (The complete schema with 7 tables, triggers, etc.)
SQL_EOF

echo "✅ SQL script created: database/library_system.sql"
echo "👉 Please run this SQL file in MySQL Workbench first!"
echo ""
echo "Next steps:"
echo "1. Copy mysql-connector-j-9.5.0.jar to lib/"
echo "2. Copy Java files to src/"
echo "3. Update DBUtil.java with your MySQL password"
echo "4. Run: ./run.sh"

echo ""
echo "Optional: Configure AI endpoint for the built-in assistant"
echo "You can export these environment variables (recommended):"
echo "  export AI_API_URL=\"https://openrouter.ai/api/v1/chat/completions\""
echo "  export AI_API_KEY=\"YOUR_API_KEY\""
echo "  export AI_MODEL=\"gpt-4o-mini\""
echo "  # OpenRouter-specific (alternative)"
echo "  export OPENROUTER_API_URL=\"https://openrouter.ai/api/v1/chat/completions\""
echo "  export YOUR_API_KEY=\"YOUR_API_KEY\""
echo "  export OPENROUTER_MODEL=\"openai/gpt-oss-20b:free\""
