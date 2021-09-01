package yoshikihigo.tinypdg.scorpio;

public class CommitRecord {
    final String commitID;
    final String author;
    final String[] filePath;

    public CommitRecord(String commitID, String author, String[] filePath) {
        this.commitID = commitID;
        this.author = author;
        this.filePath = filePath;
    }
}
