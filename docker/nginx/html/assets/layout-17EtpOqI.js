import{r as o,_ as V,j as e,L as S,u as me,f as xe,S as pe,i as $,k as fe,l as I,R as D,n as he,o as ue}from"./index-Bqd9ntWg.js";import{u as C,d as ve,e as je,a as we}from"./query.gen-DycqmmC7.js";import{p as ge}from"./avatar-DGIiPYzy.js";import{I as P,R as O}from"./ResponseCode-DASChpIM.js";import{b as z,U as be}from"./buildImageUrl-UU-AqGIw.js";import{F as h}from"./index-BesF1Zzs.js";import{R as A,a as ye,L as T}from"./ToolOutlined-Yth8yYqz.js";import{a as Ne}from"./EllipsisOutlined-DJ-lU4UH.js";import{I as _}from"./asyncToGenerator-C_d2R9__.js";import{M as ke}from"./index-BWmRGx5f.js";import{D as Se}from"./index-CVmKX9-z.js";import{I as De}from"./index-BjgghHgF.js";import{M as U,R as Ce}from"./index-BIpk_xcc.js";import{B as Pe}from"./button-w-BhhqNe.js";import{s as i}from"./index-D_2ZsiUY.js";var Oe={icon:{tag:"svg",attrs:{viewBox:"64 64 896 896",focusable:"false"},children:[{tag:"path",attrs:{d:"M408 442h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8zm-8 204c0 4.4 3.6 8 8 8h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56zm504-486H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zm0 632H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zM115.4 518.9L271.7 642c5.8 4.6 14.4.5 14.4-6.9V388.9c0-7.4-8.5-11.5-14.4-6.9L115.4 505.1a8.74 8.74 0 000 13.8z"}}]},name:"menu-fold",theme:"outlined"},Ue=function(n,d){return o.createElement(_,V({},n,{ref:d,icon:Oe}))},Te=o.forwardRef(Ue),Be={icon:{tag:"svg",attrs:{viewBox:"64 64 896 896",focusable:"false"},children:[{tag:"path",attrs:{d:"M408 442h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8zm-8 204c0 4.4 3.6 8 8 8h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56zm504-486H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zm0 632H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zM142.4 642.1L298.7 519a8.84 8.84 0 000-13.9L142.4 381.9c-5.8-4.6-14.4-.5-14.4 6.9v246.3a8.9 8.9 0 0014.4 7z"}}]},name:"menu-unfold",theme:"outlined"},Me=function(n,d){return o.createElement(_,V({},n,{ref:d,icon:Be}))},Re=o.forwardRef(Me);const{Sider:Ee,Content:Le}=T,Fe=[{required:!0,message:"请输入新密码"},{min:8,message:"密码至少需要8个字符"},{max:20,message:"密码最多不能超过20个字符"},{validator:(w,n)=>/[a-zA-Z]/.test(n)?/\d/.test(n)?Promise.resolve():Promise.reject("密码必须包含至少一个数字"):Promise.reject("密码必须包含至少一个字母")}];function Ye({children:w}){var F;const[n,d]=o.useState(!1),[m,H]=o.useState(),[B]=h.useForm(),[G,M]=o.useState(!1),[g,y]=o.useState("SETTING"),[a,W]=o.useState(),[x,N]=o.useState(""),[q,K]=o.useState([]),[R,u]=o.useState(!1),[Q,Z]=o.useState([{key:"shop",icon:e.jsx(A,{}),label:e.jsx(S,{to:`/dashboard/${m==null?void 0:m.id}/shop`,children:"agent商店"})},{key:"agents",label:"agents"}]),k=me(),l=xe().pathname,v=(F=l==null?void 0:l.split("/"))==null?void 0:F[2],{data:c,refetch:E}=C({...ve({})}),{data:j}=C({...je({headers:{"Workspace-id":v}}),enabled:!!v}),{data:b}=C({...we({})}),p=o.useMemo(()=>(b==null?void 0:b.data)||[],[b]);o.useEffect(()=>{const s=l.split("/")[2],t=p.find(r=>r.id===s)||p[0];H(t)},[l,p]),o.useEffect(()=>{var s;c!=null&&c.data&&(W(c.data),N(((s=c==null?void 0:c.data)==null?void 0:s.name)||""))},[c]),o.useEffect(()=>{if(j!=null&&j.data){const s=[{key:"shop",icon:e.jsx(A,{}),label:e.jsx(S,{to:`/dashboard/${v}/shop`,children:"agent商店"})},{key:"agents",label:"最近对话"}];j.data.map(t=>{console.log("item",t),s.push({key:`chat/${t.agentId}`,icon:e.jsx(ye,{}),label:e.jsx(S,{to:`/dashboard/${v}/chat/${t.agentId}`,children:t.name})})}),Z(s)}},[j,v]);const J=async()=>{U.confirm({title:"退出登录",content:"确定退出？",okText:"确定",cancelText:"取消",onOk:async()=>{await ue({}),I(),k(D.LOGIN),console.log("sign out")},onCancel(){console.log("sign out cancel")}})},X=()=>{y("SETTING"),M(!0)},Y=()=>{window.open(D.WORKSPACES,"_blank")};function ee(s,t){const f=new URL(s).pathname.split("/").filter(de=>de!=="");return f.length>=2&&(f[1]=t),`/${f.join("/")}`}const se=s=>{ee(window.location.href,s),console.log("workspaceId",s),k(`/dashboard/${s}`)},L=()=>{g==="PASSWORD"?y("SETTING"):M(!1),u(!1)},ae=()=>{y("PASSWORD")},te=(s,t)=>!t||B.getFieldValue("newPassword")===t?Promise.resolve():Promise.reject(new Error("两次输入的密码不一致")),oe=async s=>{var t;if(s.stopPropagation(),!x)i.error("昵称不能为空");else if(x&&x!=(a==null?void 0:a.name)){console.log("update name",x);const r=await $({query:{avatar:a==null?void 0:a.avatar,name:x}});((t=r==null?void 0:r.data)==null?void 0:t.code)===O.S_OK?(i.success("昵称修改成功"),await E(),u(!1)):i.error("昵称修改失败")}else u(!1)},ne=()=>{a&&N((a==null?void 0:a.name)||""),u(!0)},re=s=>{s.stopPropagation(),N(s.target.value.trim())},le=async s=>{U.confirm({title:"修改密码",content:"修改密码后，您将需要重新登录！",okText:"确定",cancelText:"取消",onOk:async()=>{var r,f;console.log("values",s);const t=await fe({query:{originPwd:s.originPassword,newPwd:s.newPassword}});((r=t==null?void 0:t.data)==null?void 0:r.code)===O.S_OK?(i.success("密码修改成功"),I(),k(D.LOGIN)):i.error(((f=t==null?void 0:t.data)==null?void 0:f.message)||"密码修改失败")}})};o.useEffect(()=>{const s=[{type:"divider"},{key:"1",label:e.jsx("div",{onClick:Y,className:"cursor-pointer py-1.5 px-3",children:"管理我的workspace"})},{key:"2",label:e.jsx("div",{onClick:X,className:"cursor-pointer py-1.5 px-3",children:"设置"})},{key:"3",label:e.jsx("div",{onClick:J,className:"cursor-pointer py-1.5 px-3",children:"退出登录"})}];p.length>0&&p.reverse().map(t=>{s.unshift({key:t==null?void 0:t.id,label:e.jsx("div",{children:e.jsxs("div",{onClick:()=>se(t==null?void 0:t.id),className:"flex items-center max-w-[230px] overflow-hidden py-1.5 px-3",children:[e.jsx("div",{className:"w-full cursor-pointer flex-1 text-ellipsis overflow-hidden mr-6",children:t.name}),(m==null?void 0:m.id)===t.id&&e.jsx(Ne,{size:14,className:"flex-none"})]})})})}),K(s)},[p,m]);const ce=async s=>{var r;const t=await he({body:{file:s}});return((r=t.data)==null?void 0:r.code)===O.S_OK?"/v1/file/download?filename="+t.data.data:(i.error("上传失败"),"")},ie=async s=>{console.log(s),s.file.status==="done"?(await $({query:{avatar:s.file.xhr.responseURL.split("=")[1],name:a==null?void 0:a.name}}),await E(),await i.success(`${s.file.name} 上传成功`)):s.file.status==="error"&&i.error(`${s.file.name} 上传失败`)};return e.jsxs(T,{className:"h-[100vh] overflow-hidden",children:[e.jsxs(Ee,{trigger:null,collapsible:!0,collapsed:n,onCollapse:s=>d(s),className:"pageLayoutSider bg-gray-900 py-3 flex flex-col",width:250,children:[e.jsxs("div",{className:"mt-3 px-5 flex-none flex justify-center items-center mb-9",children:[!n&&e.jsx("div",{className:"flex-1 text-lg text-white",children:"LiteAgent"}),e.jsxs("div",{className:"flex-none",children:[!n&&e.jsx(Te,{onClick:()=>d(!n),style:{fontSize:"16px",color:"#fff"}}),n&&e.jsx(Re,{onClick:()=>d(!n),style:{fontSize:"16px",color:"#fff"}})]})]}),e.jsx(ke,{theme:"dark",mode:"inline",selectedKeys:[(l.split("/")[4]?`${l.split("/")[3]}/${l.split("/")[4]}`:l.split("/")[3])||"shop"],items:Q,className:"bg-gray-900 border-r-0 !px-2 flex-1 overflow-y-auto"}),e.jsx(Se,{overlayClassName:"dashboardDropdown",menu:{items:q},placement:"top",children:e.jsxs("div",{className:"flex-none px-5 justify-center h-[40px] flex justify-center items-center",children:[e.jsx(De,{preview:!1,className:"flex-none w-6 rounded-full",src:z(a==null?void 0:a.avatar)||ge,alt:"avatar"}),!n&&e.jsx("div",{className:"flex-1 ml-3 line-clamp-1 break-all text-sm text-white cursor-pointer",children:a==null?void 0:a.name})]})}),e.jsx(U,{title:g==="SETTING"?"设置":"修改密码",closable:!0,onCancel:L,className:"!w-[538px]",footer:null,maskClosable:!1,open:G,centered:!0,children:e.jsxs("div",{className:"px-8 pt-7 pb-12 flex flex-col items-center",children:[g==="SETTING"&&e.jsxs("div",{className:"w-full",children:[e.jsxs("div",{className:"flex items-end mb-5",children:[e.jsx("div",{className:"w-[88px] flex justify-end font-xs mr-3",children:"头像："}),e.jsx("div",{className:"avatarWrapper",children:e.jsx(be,{name:"icon",maxCount:1,defaultFileList:a!=null&&a.avatar?[{uid:"",name:"",thumbUrl:z(a==null?void 0:a.avatar)}]:void 0,accept:".png,.jpg,.jpeg,.svg,.gif,.webp",listType:"picture-card",className:"avatar-uploader",showUploadList:{showDownloadIcon:!1,showRemoveIcon:!1,showPreviewIcon:!1},action:ce,onChange:ie,children:e.jsxs("div",{children:[e.jsx(Ce,{}),e.jsx("div",{style:{marginTop:8},children:a!=null&&a.avatar?"修改头像":"上传头像"})]})})})]}),e.jsxs("div",{className:"flex items-center h-[46px]",children:[e.jsx("div",{className:"w-[88px] flex justify-end font-xs mr-3 flex-none",children:"昵称："}),!R&&e.jsxs("div",{className:"flex items-center",children:[e.jsx("div",{className:"mr-2 font-xs line-clamp-1 break-all",children:a==null?void 0:a.name}),e.jsx("div",{onClick:ne,className:"font-xs text-[#1296DB] cursor-pointer flex-none",children:"修改"})]}),R&&e.jsxs("div",{className:"flex items-center font-xs",children:[e.jsx("input",{autoFocus:!0,onChange:re,value:x,className:"h-8 font-xs text-black border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none"}),e.jsx("div",{onClick:oe,className:"mx-2 flex-none font-xs cursor-pointer border border-[#D9D9D9] border-solid rounded-md py-2.5 px-4",children:"确认"}),e.jsx("div",{className:"flex-none font-xs cursor-pointer border border-[#D9D9D9] border-solid rounded-md py-2.5 px-4",onClick:()=>u(!1),children:"取消"})]})]}),e.jsx("div",{className:"w-full h-px bg-[#F2F2F2] my-6"}),e.jsxs("div",{className:"flex items-center mb-6",children:[e.jsx("div",{className:"w-[88px] flex justify-end font-xs mr-3",children:"账号："}),e.jsx("input",{disabled:!0,className:"w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none",value:a==null?void 0:a.email})]}),e.jsxs("div",{className:"flex items-center",children:[e.jsx("div",{className:"w-[88px] flex justify-end font-xs mr-3",children:"密码："}),e.jsx("div",{onClick:ae,className:"font-xs text-[#2A82E4] cursor-pointer opacity-70",children:"修改密码"})]})]}),g==="PASSWORD"&&e.jsx("div",{className:"w-full updatePassword",children:e.jsxs(h,{form:B,name:"update_password",onFinish:le,children:[e.jsx(h.Item,{name:"originPassword",rules:[{required:!0,message:"请输入旧密码!"}],children:e.jsxs("div",{className:"flex items-center",children:[e.jsx("div",{className:"w-[88px] flex justify-end font-xs mr-3",children:"旧密码："}),e.jsx(P,{className:"w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none",type:"password",placeholder:"请输入旧密码"})]})}),e.jsx(h.Item,{name:"newPassword",rules:Fe,children:e.jsxs("div",{className:"flex items-center",children:[e.jsx("div",{className:"w-[88px] flex justify-end font-xs mr-3",children:"新密码："}),e.jsx(P,{className:"w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none",type:"password",placeholder:"请输入新密码"})]})}),e.jsx(h.Item,{name:"confirmPassword",rules:[{required:!0,message:"请再次输入新密码!"},{validator:te}],children:e.jsxs("div",{className:"flex items-center",children:[e.jsx("div",{className:"w-[88px] flex justify-end font-xs mr-3",children:"新密码确认："}),e.jsx(P,{className:"w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none",type:"password",placeholder:"请再次输入新密码"})]})}),e.jsx(h.Item,{children:e.jsxs("div",{className:"flex items-center justify-end mt-10",children:[e.jsx("div",{onClick:L,className:"w-[88px] h-8 cursor-point flex items-center justify-center border border-solid border-[#D9D9D9] rounded-sm text-black/65 mr-4",children:"取消"}),e.jsx(Pe,{type:"primary",htmlType:"submit",className:"w-[88px] h-8 flex items-center justify-center border border-solid border-[#1890FF] rounded-sm bg-[#1890FF] text-white",children:"确定"})]})})]})})]})})]}),e.jsx(T,{children:e.jsx(o.Suspense,{fallback:e.jsx(pe,{}),children:e.jsx(Le,{className:"bg-white rounded-lg p-5",children:w})})})]})}export{Ye as P};