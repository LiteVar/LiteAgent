import{r as a,_ as $,f as C,u as R,j as e,S,aB as N,L as i}from"./index-Bqd9ntWg.js";import{u as h,a as L,d as O}from"./query.gen-DycqmmC7.js";import{W as M}from"./workspaceContext-DM4ldxn7.js";import{L as p,a as W,R as z}from"./ToolOutlined-Yth8yYqz.js";import{S as j}from"./index-CO55h4Px.js";import{R as E}from"./SwapOutlined-BPIwuBYF.js";import{M as A}from"./index-BWmRGx5f.js";import{I as T}from"./asyncToGenerator-C_d2R9__.js";import{R as B}from"./UserOutlined-u09pEzK8.js";import"./compact-item-BLNrSCAv.js";import"./LeftOutlined-SuJsU2gG.js";import"./useZIndex-CxNEmHgd.js";import"./EllipsisOutlined-DJ-lU4UH.js";import"./index-C81jIois.js";import"./move-C6Ql-cRB.js";var P={icon:{tag:"svg",attrs:{viewBox:"64 64 896 896",focusable:"false"},children:[{tag:"path",attrs:{d:"M824.2 699.9a301.55 301.55 0 00-86.4-60.4C783.1 602.8 812 546.8 812 484c0-110.8-92.4-201.7-203.2-200-109.1 1.7-197 90.6-197 200 0 62.8 29 118.8 74.2 155.5a300.95 300.95 0 00-86.4 60.4C345 754.6 314 826.8 312 903.8a8 8 0 008 8.2h56c4.3 0 7.9-3.4 8-7.7 1.9-58 25.4-112.3 66.7-153.5A226.62 226.62 0 01612 684c60.9 0 118.2 23.7 161.3 66.8C814.5 792 838 846.3 840 904.3c.1 4.3 3.7 7.7 8 7.7h56a8 8 0 008-8.2c-2-77-33-149.2-87.8-203.9zM612 612c-34.2 0-66.4-13.3-90.5-37.5a126.86 126.86 0 01-37.5-91.8c.3-32.8 13.4-64.5 36.3-88 24-24.6 56.1-38.3 90.4-38.7 33.9-.3 66.8 12.9 91 36.6 24.8 24.3 38.4 56.8 38.4 91.4 0 34.2-13.3 66.3-37.5 90.5A127.3 127.3 0 01612 612zM361.5 510.4c-.9-8.7-1.4-17.5-1.4-26.4 0-15.9 1.5-31.4 4.3-46.5.7-3.6-1.2-7.3-4.5-8.8-13.6-6.1-26.1-14.5-36.9-25.1a127.54 127.54 0 01-38.7-95.4c.9-32.1 13.8-62.6 36.3-85.6 24.7-25.3 57.9-39.1 93.2-38.7 31.9.3 62.7 12.6 86 34.4 7.9 7.4 14.7 15.6 20.4 24.4 2 3.1 5.9 4.4 9.3 3.2 17.6-6.1 36.2-10.4 55.3-12.4 5.6-.6 8.8-6.6 6.3-11.6-32.5-64.3-98.9-108.7-175.7-109.9-110.9-1.7-203.3 89.2-203.3 199.9 0 62.8 28.9 118.8 74.2 155.5-31.8 14.7-61.1 35-86.5 60.4-54.8 54.7-85.8 126.9-87.8 204a8 8 0 008 8.2h56.1c4.3 0 7.9-3.4 8-7.7 1.9-58 25.4-112.3 66.7-153.5 29.4-29.4 65.4-49.8 104.7-59.7 3.9-1 6.5-4.7 6-8.7z"}}]},name:"team",theme:"outlined"},V=function(s,o){return a.createElement(T,$({},s,{ref:o,icon:P}))},_=a.forwardRef(V);const{Sider:K,Content:Q}=p;function ne(){var x;const s=C().pathname,o=(x=s==null?void 0:s.split("/"))==null?void 0:x[2],[k,w]=a.useState(!1),[r,u]=a.useState(),f=R(),{data:c}=h({...L({})}),{data:l}=h({...O({})}),n=a.useMemo(()=>(c==null?void 0:c.data)||[],[c]),b=l==null?void 0:l.data;a.useEffect(()=>{const t=s.split("/")[2],d=n.find(m=>m.id===t)||n[0];u(d)},[s,n]);const y=a.useCallback(t=>{const d=n.find(I=>I.id===t);u(d);const m=`/workspaces/${t}/${s.split("/")[3]||"agents"}`;f(m)},[n,f,s]),v=[{key:"agents",icon:e.jsx(_,{}),label:e.jsx(i,{to:`/workspaces/${o}/agents`,children:"Agents管理"})},{key:"tools",icon:e.jsx(W,{}),label:e.jsx(i,{to:`/workspaces/${o}/tools`,children:"工具管理"})},{key:"models",icon:e.jsx(z,{}),label:e.jsx(i,{to:`/workspaces/${o}/models`,children:"模型管理"})},{key:"users",icon:e.jsx(B,{}),label:e.jsx(i,{to:`/workspaces/${o}/users`,children:"用户管理"})}];return e.jsxs(p,{className:"min-h-screen",children:[e.jsxs(K,{collapsible:!0,collapsed:k,onCollapse:t=>w(t),className:"bg-gray-900 text-white py-3",width:250,children:[e.jsx("div",{className:"h-8 m-4 bg-gray-800 rounded flex items-center justify-between cursor-pointer",children:e.jsx(j,{className:"w-full rp-[.ant-select-selection-item]:text-white",variant:"borderless",size:"large",dropdownStyle:{width:218},value:r==null?void 0:r.id,onChange:y,suffixIcon:e.jsx(E,{className:"text-white text-base"}),children:n.map(t=>e.jsx(j.Option,{value:t.id,children:t.name},t.id))})}),e.jsx(A,{theme:"dark",mode:"inline",selectedKeys:[s.split("/")[3]||"agents"],items:v,className:"bg-gray-900 border-r-0 !px-2"})]}),e.jsx(p,{children:e.jsx(a.Suspense,{fallback:e.jsx(S,{}),children:e.jsx(Q,{className:"m-5 p-5 bg-white rounded-lg",children:e.jsx(M,{value:{workspace:r,userInfo:b},children:e.jsx(N,{})})})})})]})}export{ne as default};