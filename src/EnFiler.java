import com.alibaba.fastjson.JSON;
import lotus.domino.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author:xukangfeng
 * @Description
 * @Date : Create in 15:10 2018/8/19
 */
public class EnFiler extends HttpServlet {

    //previewFileIconSettings
    public static Map fileIconMap=new HashMap();


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


    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        System.out.println("EnFiler 初始化...");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("EnFiler doPost Running...");
        Database mssDb=null;
        Session session=null;
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        String fileIds = request.getParameter("fileIds");
        String appMSSDatabase = request.getParameter("mssDbPath");
        String appDocUNID = request.getParameter("appDocUNID");
        List<Document> fileList=new ArrayList<Document>();
        FileResult msg = null;
        Document attDoc = null;

        //appMSSDatabase = "weboa/equipmentLib1.nsf";
        /**
         * 初始化
         * 海量库
         */
        try {
            NotesThread.sinitThread();
            session = NotesFactory.createSession();
            mssDb = session.getDatabase(session.getServerName(), appMSSDatabase);

            if(!StrUtil.isEmpty(fileIds)) {
                attDoc = mssDb.getDocumentByUNID(fileIds);
                /*
                String[] fileIdArr = fileIds.split(",");
                for (String unid : fileIdArr){
                    attDoc = mssDb.getDocumentByUNID(unid);
                    fileList.add(attDoc);
                }*/
            }

            System.out.println("海量库找到附件文档的size："+fileList.size());

            try {
                msg = EnUploader.getPreivewSettings(attDoc,request,appDocUNID);
            } catch (NotesException e) {
                e.printStackTrace();
            }


        } catch (NotesException e) {
            e.printStackTrace();
        }finally {
            try {
                NotesThread.stermThread();
                if(attDoc!=null) attDoc.recycle();
                if (mssDb!=null ) mssDb.recycle();
                if(session!=null) session.recycle();

            } catch (NotesException e) {
                e.printStackTrace();
            }
        }

        String msgReturn = JSON.toJSON(msg).toString();
        /**
         * 返回json
         */
        out.write(msgReturn);
        out.flush();
        out.close();

    }






    /**
     * 根据文件名获取icon
     * @param fileName 文件
     * @return
     */
    public String getFileIcon(String fileName){
        String ext= StrUtil.getExtName(fileName);
        return fileIconMap.get(ext)==null?fileIconMap.get("default").toString():fileIconMap.get(ext).toString();
    }

}
