import{j as e,r as m,aU as O,aV as W,aW as w}from"./index-Bqd9ntWg.js";import{M as V,R as B}from"./index-BIpk_xcc.js";import{D as b,P as N}from"./index-D2KebkZD.js";import{F as h}from"./index-BesF1Zzs.js";import{I as k,R as T}from"./ResponseCode-DASChpIM.js";import{S as _}from"./index-js1QsEy5.js";import{B as M}from"./button-w-BhhqNe.js";import{u as q,j as A}from"./query.gen-DycqmmC7.js";import{u as z}from"./workspaceContext-DM4ldxn7.js";import{U as H}from"./User-CUv3sm6S.js";import{s as x}from"./index-D_2ZsiUY.js";import{F as Q}from"./Table-C55P5jUG.js";import"./LeftOutlined-SuJsU2gG.js";import"./asyncToGenerator-C_d2R9__.js";import"./compact-item-BLNrSCAv.js";import"./useZIndex-CxNEmHgd.js";import"./render-CvJz5-Mq.js";import"./EllipsisOutlined-DJ-lU4UH.js";import"./useBreakpoint-HDRcWJZe.js";import"./index-CO55h4Px.js";import"./index-C81jIois.js";import"./move-C6Ql-cRB.js";import"./index-WUPub_KU.js";import"./index-CVmKX9-z.js";import"./index-BWmRGx5f.js";import"./index-DdR5j2rt.js";const $=({visible:p,onClose:r,modelInfo:l})=>e.jsx(V,{centered:!0,title:"模型详情",open:p,onCancel:r,footer:null,children:e.jsxs(b,{column:1,children:[e.jsx(b.Item,{label:"模型名称",children:l==null?void 0:l.name}),e.jsx(b.Item,{label:"BaseURL",children:l==null?void 0:l.baseUrl}),e.jsx(b.Item,{label:"API Key",children:l==null?void 0:l.apiKey})]})}),G=p=>{let r;try{r=new URL(p)}catch{return!1}return r.protocol==="http:"||r.protocol==="https:"},J=p=>{const{visible:r,onCancel:l,onOk:j,onDelete:u,initialData:i}=p,[c]=h.useForm(),f=!!i;m.useEffect(()=>{i?c.setFieldsValue(i):c.resetFields()},[i,c]),m.useEffect(()=>{r||c.resetFields()},[r,c]);const g=m.useCallback(async()=>{c.validateFields().then(a=>{j({...a,id:i==null?void 0:i.id})})},[c,j,i]),F=()=>{i!=null&&i.id&&u&&u(i.id)};return e.jsxs(V,{centered:!0,title:f?"编辑模型":"新建模型",open:r,onCancel:l,onOk:g,okText:"确定",cancelText:"取消",children:[e.jsxs(h,{form:c,layout:"vertical",children:[e.jsx(h.Item,{name:"name",label:"模型名称",rules:[{required:!0,message:"请输入模型名称",whitespace:!0}],children:e.jsx(k,{maxLength:20,placeholder:"请输入模型名称"})}),e.jsx(h.Item,{name:"baseUrl",label:"BaseURL",rules:[{required:!0,message:"请输入URL",whitespace:!0},()=>({validator(a,d){return!(d!=null&&d.trim())||G(d)?Promise.resolve():Promise.reject(new Error("请输入正确的URL"))}})],children:e.jsx(k,{maxLength:500,placeholder:"请输入URL"})}),e.jsx(h.Item,{name:"apiKey",label:"API Key",rules:[{required:!0,message:"请输入API Key",whitespace:!0}],children:e.jsx(k,{maxLength:60,placeholder:"请输入key值"})}),e.jsx(h.Item,{name:"maxTokens",label:"maxToken最大值",rules:[{pattern:/^[1-9]\d*$/,message:"请输入大于0的正整数"}],children:e.jsx(k,{min:1,maxLength:9,type:"number",placeholder:"请输入maxToken最大值"})}),e.jsx(h.Item,{name:"shareFlag",label:"分享设置",valuePropName:"checked",extra:e.jsx("span",{className:"ml-2",children:"未开启，开启后其他成员可以查看并使用此模型"}),children:e.jsx(_,{checkedChildren:"开启",unCheckedChildren:"关闭"})})]}),f&&u&&e.jsx(N,{title:"确认删除",description:"即将删除模型的所有信息，确认删除？",onConfirm:F,okText:"确认",cancelText:"取消",children:e.jsx(M,{danger:!0,style:{float:"left"},children:"删除"})})]})};function Fe(){var R,U;const[p,r]=m.useState(!1),[l,j]=m.useState(!1),[u,i]=m.useState(void 0),[c,f]=m.useState(0),[g,F]=m.useState(10),a=z(),{data:d,refetch:y}=q({...A({query:{pageNo:c,pageSize:g},headers:{"Workspace-id":(a==null?void 0:a.id)||""}})}),E=t=>{r(!0),i(t)},K=()=>{r(!1),i(void 0)},P=(t,s)=>{t.stopPropagation(),i(s),j(!0)},C=()=>{j(!1),i(void 0)},v=m.useCallback(async t=>{var s,n,I,L;if(t.shareFlag=!!t.shareFlag,t.id){const o=await O({body:t,headers:{"Workspace-id":(a==null?void 0:a.id)||""}});((s=o==null?void 0:o.data)==null?void 0:s.code)===T.S_OK?x.success("更新模型成功"):x.error((n=o==null?void 0:o.data)==null?void 0:n.message)}else{const o=await W({body:t,headers:{"Workspace-id":(a==null?void 0:a.id)||""}});((I=o==null?void 0:o.data)==null?void 0:I.code)===T.S_OK?x.success("创建模型成功"):x.error((L=o==null?void 0:o.data)==null?void 0:L.message)}y(),C()},[y,a]),S=m.useCallback(async t=>{var n;const s=await w({path:{id:t},headers:{"Workspace-id":(a==null?void 0:a.id)||""}});if(((n=s==null?void 0:s.data)==null?void 0:n.code)!==200){x.error("删除模型失败");return}x.success("删除模型成功"),y(),C()},[y,a]),D=[{title:"名称",dataIndex:"name",key:"name"},{title:"key值",dataIndex:"apiKey",key:"apiKey"},{title:"状态",dataIndex:"shareFlag",key:"shareFlag",render:t=>e.jsx("span",{children:t?"已分享":"-"})},{title:"操作",key:"action",render:(t,s)=>e.jsxs(e.Fragment,{children:[(s==null?void 0:s.canEdit)&&e.jsx(M,{type:"link",onClick:n=>P(n,s),children:"编辑"}),(s==null?void 0:s.canDelete)&&e.jsx(N,{title:"确认删除模型？",onConfirm:n=>{n==null||n.stopPropagation(),S(s.id)},onCancel:n=>n==null?void 0:n.stopPropagation(),okText:"确认",cancelText:"取消",children:e.jsx(M,{onClick:n=>n.stopPropagation(),type:"link",danger:!0,children:"删除"})}),!(s!=null&&s.canEdit)&&!(s!=null&&s.canDelete)&&"-"]})}];return e.jsxs("div",{className:"space-y-4",children:[e.jsxs("div",{className:"flex justify-between items-center",children:[e.jsx("h1",{className:"text-2xl font-bold",children:"模型管理"}),Number(a==null?void 0:a.role)!==H.Normal&&e.jsx(M,{type:"primary",icon:e.jsx(B,{}),onClick:t=>P(t,void 0),children:"新建模型"})]}),e.jsx(Q,{columns:D,dataSource:((R=d==null?void 0:d.data)==null?void 0:R.list)||[],onRow:t=>({onClick:()=>{t!=null&&t.canRead&&E(t)}}),pagination:{current:c+1,pageSize:g,total:Number(((U=d==null?void 0:d.data)==null?void 0:U.total)||10),onChange:(t,s)=>{f(t-1),F(s)}}}),e.jsx($,{visible:p,onClose:K,modelInfo:u}),e.jsx(J,{visible:l,onCancel:C,onOk:t=>v(t),onDelete:S,initialData:u})]})}export{Fe as default};