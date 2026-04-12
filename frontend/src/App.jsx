import { useState, useEffect, useRef } from "react";
import "./App.css";
import { marked } from "marked";
import DOMPurify from "dompurify";

function appendBotChunk(setMessages, text) {
    if (!text) {
        return;
    }

    setMessages((prev) => {
        const last = prev[prev.length - 1];

        if (last && last.sender === "BOT") {
            return [
                ...prev.slice(0, -1),
                { ...last, content: last.content + text }
            ];
        }

        return [...prev, { sender: "BOT", content: text }];
    });
}

function Header() {
    return (
        <header className="flex flex-col items-center">
            <h2 className="mt-5 mb-5 font-bold text-4xl text-center text-[#ffffff]">
                Chatbot Politechniki Łódzkiej
            </h2>
            <div className="mb-5 md:flex items-center gap-4">
                <h3 className="mb-2 md:mb-0 text-[#ffffff]">Wybierz sposób generowania pytań follow-up:</h3>
                <select
                    id="followupMethod"
                    className="px-4 py-2 rounded-lg bg-gray-200 text-black"
                >
                    <option value="RAG">RAG (Retrieval-Augmented Generation)</option>
                    <option value="PROMPT_ENGINEERING">Prompt-engineering</option>
                    <option value="TEMPLATE_BASED">Metoda szablonowa</option>
                </select>
            </div>
        </header>
    );
}

function ChatWindow({ messages, followups, onFollowupClick, isTyping, degree, onSelectDegree }) {
    const endRef = useRef(null);

    useEffect(() => {
        endRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages, followups, isTyping]);

    return (
        <div className="bg-linear-to-b from-gray-700 to-gray-600 w-5/6 grow rounded-xl shadow-lg p-6 overflow-y-auto relative">
            <ul className="pb-40">
                <li className="p-3 space-y-5 rounded-lg text-lg max-w-3xl whitespace-normal bg-[#002147] text-[#ffffff] mr-auto mb-6 leading-relaxed">
                    Jestem Tulbot, chatbot Politechniki Łódzkiej. Wybierz proszę jedną z opcji poniżej, której będą dotyczyć zadawane przez ciebie pytania, a to pomoże mi dostosować moje odpowiedzi do twoich aktualnych potrzeb 😁😁😁
                </li>
                {degree === "I" && (
                    <li className="p-3 space-y-5 rounded-lg text-lg max-w-3xl whitespace-normal bg-[#002147] text-[#ffffff] mr-auto mb-6 leading-relaxed">
                        Cześć! Mam nadzieję, że Twoja matura poszła dobrze. Cieszę się, że interesujesz się Politechniką Łódzką 💪. Chętnie pomogę Ci w wyborze kierunku i odpowiem na wszystkie pytania dotyczące studiów I stopnia 🎉🍾.
                    </li>
                )}

                {degree === "II" && (
                    <li className="p-3 space-y-5 rounded-lg text-lg max-w-3xl whitespace-normal bg-[#002147] text-[#ffffff] mr-auto mb-6 leading-relaxed">
                        Witaj! Gratuluję ukończenia studiów inżynierskich. Jeśli myślisz nad kontynuację nauki na Politechnice Łódzkiej, postaram się pomóc Ci wybrać najlepszy kierunek studiów II stopnia i rozwiać wszelkie wątpliwości 🫡🫡🫡.
                    </li>
                )}

                {degree === "all" && (
                    <li className="p-3 space-y-5 rounded-lg text-lg max-w-3xl whitespace-normal bg-[#002147] text-[#ffffff] mr-auto mb-6 leading-relaxed">
                        Świetnie! Postaram się odpowiadać możliwie uniwersalnie i pomóc Ci w każdej sprawie związanej z Politechniką Łódzką, a mówią mi, że jestem w tym całkiem niezły  😎 😎 😎.
                    </li>
                )}
                {messages.map((msg, index) => (
                    <li
                        key={index}
                        className={`p-3 rounded-lg text-lg max-w-3xl wrap-break-word whitespace-normal mb-6 leading-relaxed ${
                            msg.sender === "USER"
                                ? "bg-[#505050] text-[#ffffff] ml-auto"
                                : "bg-[#002147] text-[#ffffff] mr-auto"
                        }`}
                    >
                        {msg.sender === "BOT" ? (
                            <div
                                className="prose prose-invert max-w-none leading-[1.8] prose-p:my-2 prose-ul:pl-5 prose-ul:my-2 prose-li:my-1 prose-h2:text-lg
                            prose-h2:mt-4 prose-h2:mb-2 prose-h3:text-base prose-a:text-blue-400 prose-a:border-b prose-a:border-blue-400/60
                            hover:prose-a:text-blue-300 hover:prose-a:border-blue-300 prose-p:text-lg prose-li:text-lg text-[#ffffff]"
                                dangerouslySetInnerHTML={{
                                    __html: DOMPurify.sanitize(
                                        marked.parse(msg.content, {
                                            breaks: true,
                                            mangle: false,
                                            headerIds: false
                                        })
                                    )
                                }}
                            />
                        ) : (
                            <span>{msg.content}</span>
                        )}

                        {msg.sender === "BOT" && isTyping && index === messages.length - 1 && (
                            <div className="mt-1 flex items-center space-x-2">
                                <span>Bot pisze</span>
                                <TypingIndicator />
                            </div>
                        )}
                    </li>
                ))}
                {degree === null && (
                    <div className="mt-6 flex flex-wrap gap-3 justify-center">
                        {[
                            { label: "Studia I stopnia", value: "I" },
                            { label: "Studia II stopnia", value: "II" },
                            { label: "Pomiń", value: "all" }
                        ].map((degreeChoice, i) => (
                            <button
                                key={i}
                                onClick={() => onSelectDegree(degreeChoice.value)}
                                className="bg-[#ffc107] hover:bg-yellow-400 text-gray-900 px-4 py-2 rounded-lg transition-all hover:scale-103 shadow-md font-semibold text-lg"
                            >
                                {degreeChoice.label}
                            </button>
                        ))}
                    </div>
                )}
                {followups.length > 0 && (
                    <div className="mt-6 flex flex-wrap gap-3 justify-center">
                        {followups.map((question, i) => (
                            <button
                                key={i}
                                onClick={() => onFollowupClick(question)}
                                className="bg-[#ffc107] hover:bg-yellow-400 text-gray-900 px-4 py-2 rounded-lg transition-all hover:scale-103 shadow-md font-semibold text-lg"
                            >
                                {question}
                            </button>
                        ))}
                    </div>
                )}
            </ul>
            <div ref={endRef} />
        </div>
    );
}

function App() {
    const [messages, setMessages] = useState([]);
    const [followups, setFollowups] = useState([]);
    const [input, setInput] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [conversationId, setConversationId] = useState(null);
    const [degree, setDegree] = useState(null);
    const eventSourceRef = useRef(null);

    useEffect(() => {
        return () => {
            eventSourceRef.current?.close();
        };
    }, []);

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

        eventSourceRef.current?.close();
        setIsLoading(true);
        setMessages((prev) => [...prev, { sender: "USER", content: text }]);
        setFollowups([]);
        setInput("");
        const method = document.getElementById("followupMethod").value;

        const eventSource = new EventSource(
            `/api/conversation/ask?content=${encodeURIComponent(text)}&conversationId=${conversationId || ""}&method=${method}&level=${degree}`
        );
        eventSourceRef.current = eventSource;

        eventSource.onmessage = (event) => {
            try {
                const parsed = JSON.parse(event.data);
                switch (parsed.type) {
                    case "conversationId":
                        setConversationId(parsed.id);
                        return;
                    case "chunk":
                    case "final":
                        appendBotChunk(setMessages, parsed.text);
                        return;
                    case "followups":
                        setFollowups(parsed.options);
                        setIsLoading(false);
                        eventSource.close();
                        eventSourceRef.current = null;
                        return;
                    case "error":
                        console.error("Błąd SSE:", parsed.message);
                        setIsLoading(false);
                        eventSource.close();
                        eventSourceRef.current = null;
                        return;
                    default:
                        return;
                }
            } catch (err) {
                console.error("Błąd parsowania SSE:", err);
            }
        };

        eventSource.onerror = () => {
            setIsLoading(false);
            eventSource.close();
            eventSourceRef.current = null;
        };
    };

    return (
        <div className="min-h-screen flex flex-col bg-[#002147]">
            <Header />
            <main className="flex flex-col items-center grow pb-4 w-full">
                <ChatWindow
                    messages={messages}
                    followups={followups}
                    onFollowupClick={handleFollowupClick}
                    isTyping={isLoading}
                    degree={degree}
                    onSelectDegree={setDegree}
                />
                <div className="w-5/6 flex mt-4">
                    <input
                        type="text"
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyDown={handleKeyDown}
                        disabled={isLoading || degree === null}
                        className="grow sm:p-4 p-3  text-lg rounded-l-lg border-none outline-none bg-gray-200 text-[#2f2e31] placeholder-[#2f2e31] disabled:opacity-50"
                        placeholder={isLoading ? "Bot pisze..." : "Wpisz wiadomość..."}
                    />
                    <button
                        onClick={() => sendMessage(input)}
                        disabled={isLoading || !input.trim() || degree === null}
                        className="bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 text-white sm:px-6 px-3 rounded-r-lg transition-all"
                    >
                        {isLoading ? "..." : "Wyślij"}
                    </button>
                </div>
            </main>
            <Footer />
        </div>
    );
}

function TypingIndicator() {
    return (
        <div className="flex space-x-1 ml-2">
            <span className="w-2 h-2 bg-white rounded-full animate-bounce delay-0"></span>
            <span className="w-2 h-2 bg-white rounded-full animate-bounce delay-150"></span>
            <span className="w-2 h-2 bg-white rounded-full animate-bounce delay-300"></span>
        </div>
    );
}

function Footer() {
    return (
        <footer className="p-2 text-center text-sm text-white bg-gray-900">
            © 2025 TUL Chatbot
        </footer>
    );
}

export default App;
