import{r as s,j as t,n as P}from"./index-Bqd9ntWg.js";import{I as C,R as T}from"./ResponseCode-DASChpIM.js";import{b as V,U as E}from"./buildImageUrl-UU-AqGIw.js";import{F as l}from"./index-BesF1Zzs.js";import{M as N,R as B}from"./index-BIpk_xcc.js";import{I as D}from"./index-BjgghHgF.js";import{s as I}from"./index-D_2ZsiUY.js";const{TextArea:$}=C,q=u=>new Promise((g,c)=>{const a=new FileReader;a.readAsDataURL(u),a.onload=()=>g(a.result),a.onerror=n=>c(n)}),W=u=>{const{visible:g,onCancel:c,onOk:a,agent:n,onEdit:h}=u,[o]=l.useForm(),[d,m]=s.useState(""),[A,x]=s.useState(!1),[w,f]=s.useState(""),[y,v]=s.useState([]),i=s.useMemo(()=>!!n,[n]),F=s.useMemo(()=>i?"修改 Agent":"新建 Agent",[i]),j=s.useCallback(async()=>{try{const e=await o.validateFields();a({...e,icon:d}),o.resetFields(),m("")}catch(e){console.error("Validation failed:",e)}},[o,d,a]),b=s.useCallback(()=>{const e=o.getFieldsValue();h==null||h({...e,icon:d}),c()},[d]),U=s.useCallback(()=>{i?b():j()},[i,b,j]),k=async e=>{!e.url&&!e.preview&&(e.preview=await q(e.originFileObj)),f(e.url||e.preview),x(!0)},L=async e=>{v(e.fileList),e.file.status==="done"?(m(e.file.xhr.responseURL.split("=")[1]),await I.success(`${e.file.name} 上传成功`)):e.file.status==="error"&&(m(""),I.error(`${e.file.name} 上传失败`))},R=async e=>{var r;const p=await P({body:{file:e}});return((r=p.data)==null?void 0:r.code)===T.S_OK?"/v1/file/download?filename="+p.data.data:""},S=t.jsxs("div",{children:[t.jsx(B,{}),t.jsx("div",{style:{marginTop:8},children:"上传图标"})]}),O=s.useCallback(()=>{if(n){const{name:e,description:p,icon:r}=n;if(o.setFieldsValue({name:e,description:p}),r){const M=V(r);v([{uid:"-1",name:r,status:"done",url:M}]),m(r)}}},[n]);return s.useEffect(()=>{i&&O()},[]),t.jsx(N,{centered:!0,title:F,open:g,onCancel:c,onOk:U,okText:"确定",cancelText:"取消",children:t.jsxs(l,{form:o,layout:"vertical",children:[t.jsx(l.Item,{name:"name",label:"Agent名称",rules:[{required:!0,message:"请输入Agent名称",whitespace:!0}],children:t.jsx(C,{maxLength:20,placeholder:"请输入Agent名称"})}),t.jsxs(l.Item,{label:"图标",name:"icon",children:[t.jsx(E,{name:"icon",maxCount:1,accept:".png,.jpg,.jpeg,.svg,.gif,.webp",listType:"picture-card",className:"avatar-uploader",showUploadList:!0,action:R,onChange:L,onPreview:k,fileList:y,children:S}),w&&t.jsx(D,{wrapperStyle:{display:"none"},preview:{visible:A,onVisibleChange:e=>x(e),afterOpenChange:e=>!e&&f("")},src:w})]}),t.jsx(l.Item,{name:"description",label:"描述",children:t.jsx($,{maxLength:200,rows:4,placeholder:"用简单几句话将Agent介绍给用户"})})]})})};export{W as C};