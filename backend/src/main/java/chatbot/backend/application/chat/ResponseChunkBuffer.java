package chatbot.backend.application.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResponseChunkBuffer {
    private static final int TARGET_CHUNK_SIZE = 40;
    private static final int MAX_BUFFER_SIZE = 80;
    private final StringBuilder buffer = new StringBuilder();
    private final StringBuilder responseBuilder = new StringBuilder();

    public ResponseChunkBuffer(){}

    private Optional<String> takeReadyChunk(StringBuilder buffer) {
        if (buffer.length() < TARGET_CHUNK_SIZE) {
            return Optional.empty();
        }

        int splitIndex = findLastSeparator(buffer);

        if (splitIndex < TARGET_CHUNK_SIZE && buffer.length() < MAX_BUFFER_SIZE) {
            return Optional.empty();
        }

        if (splitIndex <= 0) {
            String chunk = buffer.toString();
            buffer.setLength(0);
            return Optional.of(chunk);
        }

        String chunk = buffer.substring(0, splitIndex);
        buffer.delete(0, splitIndex);
        return Optional.of(chunk);
    }

    private int findLastSeparator(StringBuilder buffer) {
        for (int index = buffer.length() - 1; index >= 0; index--) {
            if (isSeparator(buffer.charAt(index))) {
                return index + 1;
            }
        }

        return -1;
    }

    private boolean isSeparator(char character) {
        return Character.isWhitespace(character) || ",.;:!?)]}\"".indexOf(character) >= 0;
    }

    public List<String> append(String chunk) {
        buffer.append(chunk);

        List<String> chunks = new ArrayList<>();
        Optional<String> nextChunk = takeReadyChunk(buffer);

        while (nextChunk.isPresent()) {
            String text = nextChunk.get();

            responseBuilder.append(text);
            chunks.add(text);

            nextChunk = takeReadyChunk(buffer);
        }

        return chunks;
    }

    public Optional<String> flush() {
        if (buffer.isEmpty()) {
            return Optional.empty();
        }

        String remainingText = buffer.toString();
        buffer.setLength(0);
        responseBuilder.append(remainingText);

        return Optional.of(remainingText);
    }

    public String getFullResponse() {
        return responseBuilder.toString();
    }
}
