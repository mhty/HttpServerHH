package HttpServerHH.FileReader;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;


public class FileReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileReader.class);

    private final static String DEFAULT_CHARSET_NAME = "utf-8";
    private static FileCash cash = new FileCash();

    private Path path;
    private Path defaultPath;
    private String indexFile;
    private boolean flagNotFound = false;
    private String charsetName;


    public void setCharset(String charsetName) {
        this.charsetName = charsetName;
    }

    private FileReader(String filename) {
        this.path = Paths.get(filename);
        flagNotFound = false;
        charsetName = DEFAULT_CHARSET_NAME;
    }

    public static FileReader createFileReader(String filename) {
        return new FileReader(filename);
    }

    public ByteBuffer read(){
        pathCheck();
        try {
            if (cash.contains(getCashKey())) {
                return ByteBuffer.wrap(cash.getPage(getCashKey()));
            } else {
                ByteBuffer page = readThroughCharset();
                byte[] toCash = new byte[page.remaining()];
                page.rewind();
                page.get(toCash);
                cash.addPage(getCashKey(), toCash);
                page.rewind();
                return page;
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read file " + path);
        }
    }

    private String getCashKey() {
        return path.toString() + " " + charsetName;
    }

    private ByteBuffer readThroughCharset() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(Files.readAllBytes(path));
        Charset defaultCharset = Charset.forName(DEFAULT_CHARSET_NAME);
        Charset charset = Charset.forName(charsetName);
        CharBuffer charBuffer = defaultCharset.decode(byteBuffer);
        return charset.encode(charBuffer);
    }

    public void pathCheck() {
        if (Files.notExists(path)) {
            LOGGER.warn("Path {}  doesn't exists, read from default path {}", path.toString(), defaultPath.toString());
            path = defaultPath;
            flagNotFound = true;
        }
        if (Files.isDirectory(path)) {
            LOGGER.warn("Path {} is directory, used index file {}", path.toString(), indexFile);
            path = Paths.get(path.toString(), indexFile);
        }
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = Paths.get(defaultPath);
        if (Files.notExists(this.defaultPath)) {
            LOGGER.error("Default oath doesn't exist {}", defaultPath);
            throw new RuntimeException("Default oath doesn't exist " + defaultPath);
        }
    }

    public void setIndexFile(String indexFile) {
        this.indexFile = indexFile;
    }

    public boolean notFound() {
        return flagNotFound;
    }

    public String getLastModified() {
        long time = getLastModifiedInMs();
        final SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone(ZoneId.of("GMT")));

        String result = format.format(time);
        LOGGER.trace("Server time, last modified \n\tpath: {} \n\ttime: {}", path.toString(), result);
        return result;
    }

    private long getLastModifiedInMs() {
        long time;
        try {
            time = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            LOGGER.error("Cannot get last modified time", e);
            time = Calendar.getInstance().getTimeInMillis();
        }
        return time;
    }

    public String getContentType() {
        String pathString = path.toString();
        String extension = pathString.substring(pathString.lastIndexOf('.') + 1);

        if (extension.equals("jpg")) {
            return "image/jpeg";
        }
        if (extension.equals("js")) {
            return "application/javascript";
        }
        return "text/html; charset=" + charsetName;
    }

    public String getEtag() {
        return DigestUtils.md5Hex(path.toString() +  getLastModifiedInMs());
    }

    public void setCashTime(int cashTime) {
        cash.setTimeout(cashTime);
    }
}
