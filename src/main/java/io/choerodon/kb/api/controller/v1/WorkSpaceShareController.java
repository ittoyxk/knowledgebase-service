package io.choerodon.kb.api.controller.v1;

import io.choerodon.base.annotation.Permission;
import io.choerodon.kb.api.dao.PageAttachmentDTO;
import io.choerodon.kb.api.dao.PageDTO;
import io.choerodon.kb.api.dao.WorkSpaceFirstTreeDTO;
import io.choerodon.kb.app.service.PageService;
import io.choerodon.kb.app.service.WorkSpaceShareService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Created by Zenger on 2019/6/11.
 */
@RestController
@RequestMapping(value = "/v1/work_space_share")
public class WorkSpaceShareController {

    @Autowired
    private WorkSpaceShareService workSpaceShareService;
    @Autowired
    private PageService pageService;

    @Permission(permissionPublic = true)
    @ApiOperation(value = "查询分享链接的树形结构")
    @GetMapping(value = "/tree")
    public ResponseEntity<WorkSpaceFirstTreeDTO> queryTree(
            @ApiParam(value = "分享链接token", required = true)
            @RequestParam("token") String token) {
        return new ResponseEntity<>(workSpaceShareService.queryTree(token),
                HttpStatus.OK);
    }

    @Permission(permissionPublic = true)
    @ApiOperation(value = "查询分享链接的页面信息")
    @GetMapping(value = "/page")
    public ResponseEntity<PageDTO> queryPage(
            @ApiParam(value = "工作空间ID", required = true)
            @RequestParam("work_space_id") Long workSpaceId,
            @ApiParam(value = "分享链接token", required = true)
            @RequestParam("token") String token) {
        return new ResponseEntity<>(workSpaceShareService.queryPage(workSpaceId, token),
                HttpStatus.OK);
    }

    @Permission(permissionPublic = true)
    @ApiOperation(value = "查询分享链接的页面附件")
    @GetMapping(value = "/page_attachment")
    public ResponseEntity<List<PageAttachmentDTO>> queryPageAttachment(
            @ApiParam(value = "工作空间ID", required = true)
            @RequestParam("work_space_id") Long workSpaceId,
            @ApiParam(value = "分享链接token", required = true)
            @RequestParam("token") String token) {
        return new ResponseEntity<>(workSpaceShareService.queryPageAttachment(workSpaceId, token),
                HttpStatus.OK);
    }

    @Permission(permissionPublic = true)
    @ApiOperation(value = "查询分享链接的文章标题")
    @GetMapping(value = "/{id}/toc")
    public ResponseEntity<String> pageToc(
            @ApiParam(value = "页面ID", required = true)
            @PathVariable Long id) {
        return new ResponseEntity<>(pageService.pageToc(id), HttpStatus.OK);
    }

    @ResponseBody
    @Permission(permissionPublic = true)
    @ApiOperation("导出文章为pdf")
    @GetMapping(value = "/export_pdf")
    public void exportMd2Pdf(@ApiParam(value = "页面id", required = true)
                             @RequestParam Long pageId,
                             HttpServletResponse response) {
        workSpaceShareService.exportMd2Pdf(pageId, response);
    }
}
