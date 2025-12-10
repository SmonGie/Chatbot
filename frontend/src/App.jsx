import { useState, useEffect, useRef } from "react"
import "./App.css"
import ReactMarkdown from "react-markdown"

function Header() {
    return (
        <header>
            <h2 className="mt-5 mb-5 font-bold text-4xl text-center text-amber-50">
                Chatbot politechniki łódzkiej
            </h2>
        </header>
    )
}

function ChatWindow({ messages, followups, onFollowupClick }) {
    const endRef = useRef(null)

    useEffect(() => {
        endRef.current?.scrollIntoView({ behavior: "smooth" })
    }, [messages, followups])
    return (
        <div className="bg-gradient-to-b from-gray-700 to-gray-600 w-5/6 flex-grow rounded-xl shadow-lg p-6 overflow-y-auto relative">
            <ul className="text-amber-50 pb-40">
                {messages.map((msg, idx) => (
                    <li
                        key={idx}
                        className={`p-3 mb-3 rounded-lg max-w-3xl break-words ${
                            msg.sender === "BOT"
                                ? "bg-blue-700 text-white ml-auto"
                                : "bg-green-900 text-white mr-auto"
                        }`}
                    >
                        <ReactMarkdown>{msg.content}</ReactMarkdown>
                    </li>
                ))}

                {followups.length > 0 && (
                    <div className="mt-6 flex flex-wrap gap-3 justify-center">
                        {followups.map((q, i) => (
                            <button
                                key={i}
                                onClick={() => onFollowupClick(q)}
                                className="bg-gray-700 hover:bg-gray-600 text-amber-100 px-4 py-2 rounded-lg text-sm transition-all hover:scale-105 shadow-md"
                            >
                                {q}
                            </button>
                        ))}
                    </div>
                )}
            </ul>
            <div ref={endRef} />
        </div>
    )
}

function Footer() {
    return (
        <footer className="p-2 text-center text-sm text-gray-400 bg-gray-900">
            © 2025 TUL Chatbot
        </footer>
    )
}

function App() {
    const [messages, setMessages] = useState([]);
    const [followups, setFollowups] = useState([]);
    const [input, setInput] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [conversationId, setConversationId] = useState(null);

    const handleFollowupClick = (question) => {
        sendMessage(question);
    };

    const handleKeyDown = (e) => {
        if (e.key === "Enter" && input.trim()) {
            sendMessage(input);
        }
    };

    const sendMessage = (text) => {
        if (!text.trim()) return;
        setIsLoading(true);
        setMessages((prev) => [...prev, { sender: "USER", content: text }]);
        setFollowups([]);
        setInput("");

        const eventSource = new EventSource(
            `http://localhost:8080/api/conversation/ask?content=${encodeURIComponent(text)}&conversationId=${conversationId || ""}`
        );

        eventSource.onmessage = (event) => {
            try {
                let data  = event.data;

                if (data.startsWith("data:")) {
                    data = data.slice(5).trim();
                }

                if (data.startsWith("{")) {
                    const parsed = JSON.parse(data);
                    if (parsed.type === "conversationId") {
                        setConversationId(parsed.id);
                        return;
                    }
                    if (parsed.type === "followups") {
                        setFollowups(parsed.options);
                        setIsLoading(false);
                        eventSource.close();
                    }
                } else if (data.length > 0) {
                    setMessages((prev) => {
                        const last = prev[prev.length - 1];
                        if (last && last.sender === "BOT") {
                            return [
                                ...prev.slice(0, -1),
                                { ...last, content: last.content + data }
                            ];

                        } else {
                            return [...prev, { sender: "BOT", content: data }];
                        }
                    });
                }
            } catch (err) {
                console.error("Błąd parsowania SSE:", err);
            }
        };

        eventSource.onerror = () => {
            setIsLoading(false);
            eventSource.close();
        };
    };

    return (
        <div className="min-h-screen flex flex-col bg-gray-900">
            <Header />
            <main className="flex flex-col items-center flex-grow pb-4 w-full">
                <ChatWindow
                    messages={messages}
                    followups={followups}
                    onFollowupClick={handleFollowupClick}
                />
                <div className="w-5/6 flex mt-4">
                    <input
                        type="text"
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyDown={handleKeyDown}
                        disabled={isLoading}
                        className="flex-grow p-3 rounded-l-lg border-none outline-none bg-gray-700 text-white placeholder-gray-400 disabled:opacity-50"
                        placeholder={isLoading ? "Bot pisze..." : "Wpisz wiadomość..."}
                    />
                    <button
                        onClick={() => sendMessage(input)}
                        disabled={isLoading || !input.trim()}
                        className="bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 text-white px-6 rounded-r-lg transition-all"
                    >
                        {isLoading ? "..." : "Wyślij"}
                    </button>
                </div>
            </main>
            <Footer />
        </div>
    );
}

export default App
