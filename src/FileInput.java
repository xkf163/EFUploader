import lotus.domino.Document;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @Author:xukangfeng
 * @Description
 * @Date : Create in 10:41 2018/10/1
 */
public class FileInput {



    public static Map<String,String> fileIconMap = new HashMap<String,String>();

    static {
        fileIconMap.put("doc" ,"<i class='fa fa-file-word-o text-primary'></i>");
        fileIconMap.put("docx","<i class='fa fa-file-word-o text-primary'></i>");
        fileIconMap.put("xls" ,"<i class='fa fa-file-excel-o text-success'></i>");
        fileIconMap.put("xlsx","<i class='fa fa-file-excel-o text-success'></i>");
        fileIconMap.put("ppt" ,"<i class='fa fa-file-powerpoint-o text-danger'></i>");
        fileIconMap.put("pptx","<i class='fa fa-file-powerpoint-o text-danger'></i>");
        fileIconMap.put("jpg" ,"<i class='fa fa-file-photo-o text-warning'></i>");
        fileIconMap.put("pdf" ,"<i class='fa fa-file-pdf-o text-danger'></i>");
        fileIconMap.put("zip" ,"<i class='fa fa-file-archive-o text-muted'></i>");
        fileIconMap.put("rar" ,"<i class='fa fa-file-archive-o text-muted'></i>");
        fileIconMap.put("default" ,"<i class='fa fa-file-o'></i>");

    }


    public static FileResult getPreivewSettingsUpload(String attachmentDocUNID, Map uploadMap, String appMSSDatabase, String appDocUNID) {
        FileResult fileResult=new FileResult();
        List<String> previews=new ArrayList<String>();
        List<FileResult.PreviewConfig> previewConfigs=new ArrayList<FileResult.PreviewConfig>();

        Iterator<Map.Entry<String, Long>> entries = uploadMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Long> entry = entries.next();
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            String hashOriginName = entry.getKey();
            String md5Code =hashOriginName.substring(0,hashOriginName.indexOf("_"));
            String originName = hashOriginName.substring(hashOriginName.indexOf("_")+1);
            Long fileSize =  entry.getValue();

            previews.add("<div class='kv-preview-data file-preview-other-frame'><div class='file-preview-other'>" +
                    "<span class='file-other-icon'>"+getFileIcon(originName)+"</span></div></div>");
            //上传后预览配置
            FileResult.PreviewConfig previewConfig=new FileResult.PreviewConfig();
            previewConfig.setWidth("120px");
            previewConfig.setCaption(originName);
            previewConfig.setKey(md5Code);
            previewConfig.setExtra(new FileResult.PreviewConfig.Extra(md5Code,appDocUNID,appMSSDatabase));
            previewConfig.setSize(md5Code+"_"+fileSize);
            previewConfigs.add(previewConfig);
        }

        fileResult.setInitialPreview(previews);
        fileResult.setInitialPreviewConfig(previewConfigs);
        fileResult.setFileIds(attachmentDocUNID);

        return fileResult;
    }




    /**
     * 根据文件名获取icon
     * @param fileName 文件
     * @return
     */
    public static String getFileIcon(String fileName){
        String ext= StrUtil.getExtName(fileName);
        return fileIconMap.get(ext)==null?fileIconMap.get("default").toString():fileIconMap.get(ext).toString();
    }
}
