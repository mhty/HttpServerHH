package HttpServerHH.HttpHeader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by mhty on 26.02.16.
 */
public abstract class HttpHeader {
    private HashMap<String, String> parameters;

    protected HttpHeader() {
        parameters = new HashMap<>();
    }

    abstract public void parseFirstLine(String line);

    public HttpHeader addParameter(String line) {
        String[] parsedParameter = line.split(": ", 2);
        if (parsedParameter.length < 2) {
            throw new RuntimeException("Read http request parameter exeption.\"" + line + "\"\n");
        }
        addParameter(parsedParameter[0], parsedParameter[1]);
        return this;
    }

    public HttpHeader addParameter(String key, String value) {
        parameters.put(key, value);
        return this;
    }

    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public void parseString(String headerString) {
        String[] parsedHeader = headerString.split("[\\n\\r]");

        parseFirstLine(parsedHeader[0]);
        for (int i = 1; i < parsedHeader.length; i++) {
            if (!parsedHeader[i].equals("")) {
                addParameter(parsedHeader[i]);
            }
        }
    }

    public Set<Map.Entry<String, String>> getParametersEntrySet() {
        return parameters.entrySet();
    }

    abstract protected StringBuilder getHttpHeaderFirstLine();

    @Override
    public String toString() {
        StringBuilder httpHeader = getHttpHeaderFirstLine();
        for (Map.Entry<String, String> entry : getParametersEntrySet()) {
            httpHeader
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }
        httpHeader.append("\n");
        return httpHeader.toString();
    }

    public byte[] getBytes() {
        return toString().getBytes();
    }

}
