import { useState, useEffect, useRef } from "react"
import "./App.css"

function Header() {
    return (
        <header>
            <h2 className="mt-5 mb-5 font-medium text-3xl text-center">
                Chatbot for TUL
            </h2>
        </header>
    )
}

function ChatWindow({ messages }) {
    const endRef = useRef(null)

    useEffect(() => {
        endRef.current?.scrollIntoView({ behavior: "smooth" })
    }, [messages])
    return (
        <div className="bg-gray-600 w-5/6 flex-grow rounded-lg shadow p-4 overflow-y-auto">
            <div className="text-amber-50">
                <ul>
                    {messages.map((msg, i) => (
                        <li
                            key={i}
                            className={`p-2 mb-3 rounded-lg max-w-xs bg-green-700 ${
                                msg.sender === "USER"
                                    ? "bg-blue-500 text-white ml-auto"
                                    : "bg-gray-300 text-white mr-auto"
                            }`}
                        >
                            {msg.content}
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    )
}

function MessageForm({ input, setInput, handleSubmit }) {
    return (
        <form
            className="w-full max-w-md mt-4 flex border rounded-r-lg rounded-l-lg"
            onSubmit={handleSubmit}
        >
            <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Napisz wiadomość..."
                className="flex-grow p-2 text-amber-50"
            />
            <button
                type="submit"
                className="bg-blue-500 text-white px-4 hover:bg-blue-600 rounded-r-lg"
            >
                Wyślij
            </button>
        </form>
    )
}

function Footer() {
    return (
        <footer className="p-2 text-right text-sm text-shadow-black-500">
            © 2025 TUL Chatbot
        </footer>
    )
}

function App() {
    const [messages, setMessages] = useState([])
    const [input, setInput] = useState("")
    const [conversationId, setConversationId] = useState(null)

    useEffect(() => {
        const startConversation = async () => {
            const res = await fetch("http://localhost:8080/api/conversations/start", {
                method: "POST",
            })
            const data = await res.json()
            setConversationId(data.id)
            setMessages([{ sender: "BOT", content: "Cześć 👋, w czym mogę pomóc?" }])
        }

        startConversation()
    }, [])

    const handleSubmit = async (e) => {
        e.preventDefault()
        if (!input.trim()) return

        const messageToSend = {
            conversationId: conversationId,
            sender: "USER",
            content: input,
        }

        const res = await fetch(
            `http://localhost:8080/api/conversations/${conversationId}/messages`,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(messageToSend),
            }
        )

        const updatedConversation = await res.json()
        setMessages(updatedConversation.messages || [])
        setInput("")
    }

    return (
        <div className="min-h-screen flex flex-col">
            <Header />
            <main className="flex flex-col items-center flex-grow pb-4">
                <ChatWindow messages={messages} />
                <MessageForm
                    input={input}
                    setInput={setInput}
                    handleSubmit={handleSubmit}
                />
            </main>
            <Footer />
        </div>
    )
}

export default App
