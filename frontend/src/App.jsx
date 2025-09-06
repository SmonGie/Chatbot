import { useState, useEffect } from 'react'
import './App.css'

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
            setMessages([{ sender: "bot", text: "Cześć 👋, w czym mogę pomóc?" }])
        }

        startConversation()
    }, [])


    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!input.trim()) return;

        const messageToSend = {
            conversationId: conversationId,
            sender: "USER",
            content: input,
        };

        const res = await fetch(
            `http://localhost:8080/api/conversations/${conversationId}/messages`,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(messageToSend),
            }
        );

        const updatedConversation = await res.json();
        setMessages(updatedConversation.messages || []);
        setInput("");
    };

    return (
        <>
            <div className="min-h-screen flex flex-col items-center p-4">
              <h2 className="mb-10">Chatbot for TUL</h2>
              <div className="bg-gray-600 w-full flex-grow rounded-lg shadow p-4 overflow-y-auto">
                  <div className="text-amber-50">
                      <ul>
                          {messages.map((msg, i) => (
                              <li
                                  key={i}
                                  className={`p-2 rounded-lg max-w-xs ${
                                      msg.sender === "user"
                                          ? "bg-blue-500 text-white ml-auto"
                                          : "bg-gray-300 text-black mr-auto"
                                  }`}
                              >
                                  {msg.content}
                              </li>
                          ))}
                      </ul>
                  </div>
              </div>
              <form className="w-full max-w-md mt-4 flex"  onSubmit={handleSubmit}>
                  <input
                      type="text"
                      value={input}
                      onChange={(e) => setInput(e.target.value)}
                      placeholder="Napisz wiadomość..."
                      className="flex-grow p-2 border rounded-l-lg text-amber-50"
                  />
                  <button
                      type="submit"
                      className="bg-blue-500 text-white px-4 rounded-r-lg hover:bg-blue-600"
                  >
                      Wyślij
                  </button>
              </form>
          </div>
        </>
      )
    }

export default App
