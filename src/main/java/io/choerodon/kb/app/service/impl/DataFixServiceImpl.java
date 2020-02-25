package io.choerodon.kb.app.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import io.choerodon.core.exception.CommonException;
import io.choerodon.kb.api.vo.InitKnowledgeBaseTemplateVO;
import io.choerodon.kb.api.vo.KnowledgeBaseInfoVO;
import io.choerodon.kb.api.vo.PageCreateVO;
import io.choerodon.kb.api.vo.ProjectDTO;
import io.choerodon.kb.app.service.DataFixService;
import io.choerodon.kb.app.service.KnowledgeBaseService;
import io.choerodon.kb.app.service.PageService;
import io.choerodon.kb.infra.dto.WorkSpaceDTO;
import io.choerodon.kb.infra.feign.BaseFeignClient;
import io.choerodon.kb.infra.feign.vo.OrganizationDTO;
import io.choerodon.kb.infra.mapper.WorkSpaceMapper;
import io.choerodon.kb.infra.utils.HtmlUtil;

/**
 * @author: 25499
 * @date: 2020/1/6 18:05
 * @description:
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DataFixServiceImpl implements DataFixService {
    private static final Logger logger = LoggerFactory.getLogger(DataFixServiceImpl.class);

    @Autowired
    private WorkSpaceMapper workSpaceMapper;
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    @Autowired
    private BaseFeignClient baseFeignClient;
    @Autowired
    private PageService pageService;

    @Autowired
    private ModelMapper modelMapper;
    @Override
    @Async
    public void fixData() {
        logger.info("==============================>>>>>>>> Data Fix Start <<<<<<<<=================================");
        //1.修复组织workspace
        fixOrgWorkSpace();
        //2.修复项目workspace
        fixProWorkSpace();
        //3.initKnowledgeBaseTemplate
        initKnowledgeBaseTemplate();
        logger.info("==========================>>>>>>>> Data Fix Succeed!!! FINISHED!!! <<<<<<<<========================");
    }

    private void fixOrgWorkSpace() {
        List<WorkSpaceDTO> orgWorkSpaceDTOS = workSpaceMapper.selectAllWorkSpace("org");
        logger.info("=======================>>>workSpace in Org number:{}===============>>>", orgWorkSpaceDTOS.size());
        if (!CollectionUtils.isEmpty(orgWorkSpaceDTOS)) {
            Set<Long> longs = orgWorkSpaceDTOS.stream().map(WorkSpaceDTO::getOrganizationId).collect(Collectors.toSet());
            //组织
            List<OrganizationDTO> organizationDTOList = baseFeignClient.queryByIds(longs).getBody();
            if (!CollectionUtils.isEmpty(organizationDTOList)) {
                organizationDTOList.forEach(e -> {
                    KnowledgeBaseInfoVO knowledgeBaseInfoVO = new KnowledgeBaseInfoVO();
                    knowledgeBaseInfoVO.setDescription("组织下默认知识库");
                    knowledgeBaseInfoVO.setName(e.getName());
                    knowledgeBaseInfoVO.setOpenRange("range_public");
                    KnowledgeBaseInfoVO baseInfoVO = knowledgeBaseService.create(e.getId(), null, knowledgeBaseInfoVO);
                    workSpaceMapper.updateWorkSpace(e.getId(), null, baseInfoVO.getId());
                });
            }
        }
    }

    private void fixProWorkSpace() {
        List<WorkSpaceDTO> projectWorkspace = workSpaceMapper.selectAllWorkSpace("pro");
        if (!CollectionUtils.isEmpty(projectWorkspace)) {
            logger.info("=======================>>>workSpace in pro number:{}===============>>>", projectWorkspace.size());
            Set<Long> projectList = projectWorkspace.stream().map(WorkSpaceDTO::getProjectId).collect(Collectors.toSet());
            List<ProjectDTO> projectDTOS = baseFeignClient.queryProjectByIds(projectList).getBody();
            if (!CollectionUtils.isEmpty(projectDTOS)) {
                projectDTOS.forEach(e -> {
                    KnowledgeBaseInfoVO knowledgeBaseInfoVO = new KnowledgeBaseInfoVO();
                    knowledgeBaseInfoVO.setDescription("项目下默认知识库");
                    knowledgeBaseInfoVO.setName(e.getName());
                    knowledgeBaseInfoVO.setOpenRange("range_private");
                    KnowledgeBaseInfoVO baseInfoVO = knowledgeBaseService.create(e.getOrganizationId(), e.getId(), knowledgeBaseInfoVO);
                    workSpaceMapper.updateWorkSpace(e.getOrganizationId(), e.getId(), baseInfoVO.getId());
                    workSpaceMapper.updateWorkSpace(null, e.getId(), baseInfoVO.getId());
                });
            }
        }
    }

    private void initKnowledgeBaseTemplate() {
        List<InitKnowledgeBaseTemplateVO>  list = buildInitData();
        logger.info("=======================>>>Init knowledgeBaseTemplate:{}", list.size());
        if (!CollectionUtils.isEmpty(list)) {
            list.forEach(v -> {
                v .setOpenRange("range_public");
                // 创建知识库
                KnowledgeBaseInfoVO knowledgeBaseInfoVO = knowledgeBaseService.create(0L, 0L, modelMapper.map(v, KnowledgeBaseInfoVO.class));
                List<PageCreateVO> templatePage = v.getTemplatePage();
                if (!CollectionUtils.isEmpty(templatePage)) {
                    // 创建知识库下面的模板
                    templatePage.forEach(pageCreateVO -> {
                        pageCreateVO.setBaseId(knowledgeBaseInfoVO.getId());
                        pageService.createPageWithContent(0L, 0L, pageCreateVO);
                    });
                }
            });
        }
    }

    private List<InitKnowledgeBaseTemplateVO>  buildInitData()  {
        List<InitKnowledgeBaseTemplateVO>  list = new ArrayList<>();
        try{
            String template = HtmlUtil.loadHtmlTemplate("/htmlTemplate/InitTemplate.html");
            String[] split = template.split("<div/>");
            InitKnowledgeBaseTemplateVO knowledgeBaseTemplateA = new InitKnowledgeBaseTemplateVO();
            knowledgeBaseTemplateA.setName("会议记录");
            List<PageCreateVO> pageCreateVOSA = new ArrayList<>();
            pageCreateVOSA.add(new PageCreateVO(0L,"会议纪要", split[2]));
            pageCreateVOSA.add(new PageCreateVO(0L,"产品规划会",split[4]));
            pageCreateVOSA.add(new PageCreateVO(0L,"敏捷迭代回顾会议",split[5]));
            knowledgeBaseTemplateA.setTemplatePage(pageCreateVOSA);
            list.add(knowledgeBaseTemplateA);

            InitKnowledgeBaseTemplateVO knowledgeBaseTemplateB = new InitKnowledgeBaseTemplateVO();
            knowledgeBaseTemplateB.setName("产品研发");
            List<PageCreateVO> pageCreateVOSB = new ArrayList<>();
            pageCreateVOSB.add(new PageCreateVO(0L,"技术文档",split[1]));
            pageCreateVOSB.add(new PageCreateVO(0L,"竞品分析",split[3]));
            pageCreateVOSB.add(new PageCreateVO(0L,"产品需求文档PRD",split[0]));
            knowledgeBaseTemplateB.setTemplatePage(pageCreateVOSB);
            list.add(knowledgeBaseTemplateB);

            InitKnowledgeBaseTemplateVO knowledgeBaseTemplateC = new InitKnowledgeBaseTemplateVO();
            knowledgeBaseTemplateC.setName("产品测试");
            List<PageCreateVO> pageCreateVOSC = new ArrayList<>();
            pageCreateVOSC.add(new PageCreateVO(0L,"测试文档模板设计",split[6]));
            knowledgeBaseTemplateC.setTemplatePage(pageCreateVOSC);
            list.add(knowledgeBaseTemplateC);
        }catch (IOException e){
            throw new CommonException(e);
        }
        return  list;
    }

}