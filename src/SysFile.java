import java.util.Date;

/**
 * @Author:xukangfeng
 * @Description
 * @Date : Create in 11:59 2018/8/19
 */
public class SysFile {

    protected String id;


    protected Integer version;

    protected Date createDateTime;

    protected Date updateDateTime;

    /**
     * 删除标记(0启用，1禁用)
     */
    private Integer deleted;


    private String fileName;

    private String savedName;

    private String filePath;

    private Long fileSize;

    private String createUserId;

    //业务文档UNID
    private String appDocUNID;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Date getCreateDateTime() {
        return createDateTime;
    }

    public void setCreateDateTime(Date createDateTime) {
        this.createDateTime = createDateTime;
    }

    public Date getUpdateDateTime() {
        return updateDateTime;
    }

    public void setUpdateDateTime(Date updateDateTime) {
        this.updateDateTime = updateDateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSavedName() {
        return savedName;
    }

    public void setSavedName(String savedName) {
        this.savedName = savedName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public String getAppDocUNID() {
        return appDocUNID;
    }

    public void setAppDocUNID(String appDocUNID) {
        this.appDocUNID = appDocUNID;
    }

    @Override
    public String toString() {
        return "SysFile{" +
                ", fileName='" + fileName + '\'' +
                ", savedName='" + savedName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", createUserId='" + createUserId + '\'' +
                ", appDocUNID='" + appDocUNID + '\'' +
                '}';
    }
}
