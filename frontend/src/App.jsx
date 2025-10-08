import { useState, useEffect, useRef } from "react"
import "./App.css"

function Header() {
    return (
        <header>
            <h2 className="mt-5 mb-5 font-bold text-4xl text-center">
                Chatbot for TUL
            </h2>
        </header>
    )
}

function QuestionBox({ text }) {
    return (
        <div className="bg-gray-700 hover:bg-gray-600 text-white p-3 rounded-xl shadow-md cursor-pointer transition">
            {text}
        </div>
    );
}

function ChatWindow({ messages }) {
    const endRef = useRef(null)

    useEffect(() => {
        endRef.current?.scrollIntoView({ behavior: "smooth" })
    }, [messages])
    return (
        <div className="bg-gradient-to-b from-gray-600 to-gray-800 w-5/6 flex-grow rounded-xl shadow-lg p-6 overflow-y-auto relative">
            <div className="text-amber-50">
                <ul>
                    {messages.map((msg) => (
                        <li
                            key={msg.index}
                            className={`p-2 mb-3 rounded-lg max-w-xs bg-green-700 ${
                                msg.sender === "BOT"
                                    ? "bg-blue-500 text-white ml-auto"
                                    : "bg-gray-300 text-white mr-auto"
                            }`}
                        >
                            {msg.content}
                        </li>
                    ))}
                </ul>
            </div>
            <div className="absolute bottom-4 left-0 w-full px-6">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-center">
                    <QuestionBox text="Informacje o rekrutacji" />
                    <QuestionBox text="Kierunki studiów" />
                    <QuestionBox text="Kontakt z uczelnią" />
                </div>
            </div>
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
    const [messages, setMessages] = useState([])

    useEffect(() => {
        const startConversation = async () => {
            setMessages([{ sender: "BOT", content: "Cześć 👋, w czym mogę pomóc?" }])
        }

        startConversation()
    }, [])


    return (
        <div className="min-h-screen flex flex-col">
            <Header />
            <main className="flex flex-col items-center flex-grow pb-4">
                <ChatWindow messages={messages} />
            </main>
            <Footer />
        </div>
    )
}

export default App
