import React, { Component } from 'react';
import { withRouter, Prompt } from 'react-router-dom';
import {
  Button, Modal,
} from 'choerodon-ui';
import 'codemirror/lib/codemirror.css';
import 'tui-editor/dist/tui-editor.min.css';
import 'tui-editor/dist/tui-editor-contents.min.css';
import 'tui-color-picker/dist/tui-color-picker.min.css';

import 'tui-editor/dist/tui-editor-extScrollSync';
import 'tui-editor/dist/tui-editor-extColorSyntax';
import 'tui-editor/dist/tui-editor-extUML';
import 'tui-editor/dist/tui-editor-extChart';
import 'tui-editor/dist/tui-editor-extTable';

import { Editor } from '@toast-ui/react-editor';
import uploadImage from '../../api/FileApi';
import { convertBase64UrlToBlob } from '../../common/utils';
import DocImageEditor from '../DocImageEditor';
import './DocEditor.scss';

class DocEditor extends Component {
  constructor(props) {
    super(props);
    this.state = {
      imageEditorVisible: false,
      image: false,
      callback: false,
      changeCount: -1,
    };
  }

  componentDidMount() {
    window.addEventListener('beforeunload', this.beforeClose);
    window.addEventListener('keydown', this.onKeyDown);
  }

  componentWillUnmount() {
    window.removeEventListener('beforeunload', this.beforeClose);
    window.removeEventListener('keydown', this.onKeyDown);
  }

  editorRef = React.createRef();

  beforeClose = (e) => {
    // 已无法自定义提示信息，由浏览器通用确认信息代替
    const { changeCount } = this.state;
    if (changeCount === 1) {
      e.preventDefault();
      e.returnValue = '你这在编辑的内容尚未保存，确定离开吗？';
      return '你这在编辑的内容尚未保存，确定离开吗？';
    }
  };

  onKeyDown = (e) => {
    const keyCode = e.keyCode || e.which || e.charCode;
    const ctrlKey = e.ctrlKey || e.metaKey;
    if (ctrlKey && keyCode === 83) {
      e.preventDefault();
      this.handleSave('edit');
      return false;
    }
  };

  handleSave = (type) => {
    const { onSave, onChange } = this.props;
    this.setState({
      changeCount: 0,
    });
    if (onChange) {
      onChange(false);
    }
    if (onSave) {
      const md = this.editorRef.current.editorInst.getMarkdown();
      onSave(md, type);
    }
  };

  onPasteOrUploadIamge = (file, callback) => {
    this.setState({
      imageEditorVisible: true,
      image: file,
      callback,
    });
  };

  handleImageCancel = () => {
    this.setState({
      imageEditorVisible: false,
      image: false,
      callback: false,
    });
  };

  handleImageSave = (data) => {
    const { callback } = this.state;
    const formData = new FormData();
    formData.append('file', convertBase64UrlToBlob(data), 'blob.png');
    uploadImage(formData).then((res) => {
      if (res && !res.failed) {
        callback(res[0], 'image');
        this.handleImageCancel();
      }
    });
  };

  render() {
    const {
      onCancel, data, initialEditType = 'markdown',
      hideModeSwitch = false, height = 'calc(100% - 70px)',
      comment = false, onChange,
    } = this.props;
    const { imageEditorVisible, image, changeCount } = this.state;

    let toolbarItems = [
      'heading',
      'bold',
      'italic',
      'strike',
      'divider',
      'hr',
      'quote',
      'divider',
      'ul',
      'ol',
      'task',
      'indent',
      'outdent',
      'divider',
      'table',
      'image',
      'link',
      'divider',
      'code',
      'codeblock',
    ];

    if (comment) {
      toolbarItems = [
        'heading',
        'bold',
        'italic',
        'strike',
        'hr',
        'quote',
        'task',
        'image',
        'link',
      ];
    }

    return (
      <div className="c7n-docEditor">
        <Editor
          toolbarItems={toolbarItems}
          hideModeSwitch={hideModeSwitch}
          usageStatistics={false}
          initialValue={data}
          previewStyle="vertical"
          height={height}
          initialEditType={initialEditType}
          useCommandShortcut={false}
          language="zh"
          ref={this.editorRef}
          exts={[
            {
              name: 'chart',
              minWidth: 100,
              maxWidth: 600,
              minHeight: 100,
              maxHeight: 300,
            },
            'scrollSync',
            'colorSyntax',
            'table',
          ]}
          hooks={
            {
              // 图片上传的 hook
              addImageBlobHook: (file, callback) => {
                this.onPasteOrUploadIamge(file, callback);
              },
              change: () => {
                // 第一次渲染会默认触发change
                const { changeCount: count } = this.state;
                if (!comment && count <= 0) {
                  this.setState({
                    changeCount: count + 1,
                  });
                  // 通知父组件，文章被修改
                  if (count === 0) {
                    if (onChange) {
                      onChange(true);
                    }
                  }
                }
              },
            }
          }
        />
        {/* 底部按钮应该作为参数传入，考虑到目前其它不会使用，暂不修改 */}
        {comment
          ? (
            <div className="c7n-docEditor-comment-control">
              <Button
                className="c7n-docEditor-btn"
                type="primary"
                onClick={() => this.handleSave('comment')}
              >
                <span>保存</span>
              </Button>
              <Button
                type="primary"
                onClick={onCancel}
              >
                <span>取消</span>
              </Button>
            </div>
          ) : (
            <div className="c7n-docEditor-control">
              <Button
                className="c7n-docEditor-btn"
                type="primary"
                funcType="raised"
                onClick={() => this.handleSave('edit')}
              >
                <span>保存并继续</span>
              </Button>
              <Button
                className="c7n-docEditor-btn"
                funcType="raised"
                onClick={() => this.handleSave('save')}
              >
                <span>保存</span>
              </Button>
              <Button
                funcType="raised"
                onClick={onCancel}
              >
                <span>取消</span>
              </Button>
            </div>
          )
        }
        {imageEditorVisible
          ? (
            <Modal
              visible={imageEditorVisible}
              width={1024}
              height={700}
              footer={null}
              closable={false}
            >
              <DocImageEditor
                data={image}
                onSave={this.handleImageSave}
                onCancel={this.handleImageCancel}
              />
            </Modal>
          )
          : null
        }
        <Prompt
          when={changeCount === 1}
          message={`编辑提示${Choerodon.STRING_DEVIDER}你这在编辑的内容尚未保存，确定离开吗？`}
        />
      </div>
    );
  }
}
export default withRouter(DocEditor);
