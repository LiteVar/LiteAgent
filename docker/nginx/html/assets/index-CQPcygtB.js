import{r as n,_ as l,e as d,f as x,j as e,h,s as p}from"./index-Bqd9ntWg.js";import{L as u}from"./logo-DI8TcUuy.js";import{I as c,R as f}from"./ResponseCode-DASChpIM.js";import{I as g}from"./index-BjgghHgF.js";import{F as r}from"./index-BesF1Zzs.js";import{R as j}from"./UserOutlined-u09pEzK8.js";import{I as w}from"./asyncToGenerator-C_d2R9__.js";import{C as v}from"./index-WUPub_KU.js";import{B as y}from"./button-w-BhhqNe.js";import{s as I}from"./index-D_2ZsiUY.js";import"./LeftOutlined-SuJsU2gG.js";import"./compact-item-BLNrSCAv.js";import"./useZIndex-CxNEmHgd.js";import"./SwapOutlined-BPIwuBYF.js";import"./render-CvJz5-Mq.js";var N={icon:{tag:"svg",attrs:{viewBox:"64 64 896 896",focusable:"false"},children:[{tag:"path",attrs:{d:"M832 464h-68V240c0-70.7-57.3-128-128-128H388c-70.7 0-128 57.3-128 128v224h-68c-17.7 0-32 14.3-32 32v384c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V496c0-17.7-14.3-32-32-32zM332 240c0-30.9 25.1-56 56-56h248c30.9 0 56 25.1 56 56v224H332V240zm460 600H232V536h560v304zM484 701v53c0 4.4 3.6 8 8 8h40c4.4 0 8-3.6 8-8v-53a48.01 48.01 0 10-56 0z"}}]},name:"lock",theme:"outlined"},b=function(a,t){return n.createElement(w,l({},a,{ref:t,icon:N}))},L=n.forwardRef(b);const R="redirect",k=()=>{const[o]=d(),a=x(),t=o.get(R),s=a.pathname;return!t||t===s?"/dashboard":t},K=()=>{const o=k(),a=async t=>{var m,i;const s=await h({query:{email:t.username,password:t.password}});((m=s==null?void 0:s.data)==null?void 0:m.code)===f.S_OK?(p(s.data.data),window.location.href=o):I.error((i=s==null?void 0:s.data)==null?void 0:i.message)};return e.jsxs("div",{className:"min-h-screen bg-gray-50 flex flex-col justify-center sm:px-6 lg:px-8",children:[e.jsxs("div",{className:"sm:mx-auto sm:w-full sm:max-w-md flex items-center justify-center",children:[e.jsx(g,{preview:!1,className:"mr-8 h-12 w-auto",src:u,alt:"LiteAgent"}),e.jsx("h2",{className:"mt-6 text-center text-3xl font-extrabold text-gray-900",children:"LiteAgent"})]}),e.jsx("div",{className:"mt-8 sm:mx-auto sm:w-full sm:max-w-md",children:e.jsx("div",{className:"bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10",children:e.jsxs(r,{name:"normal_login",className:"login-form",initialValues:{remember:!0},onFinish:a,children:[e.jsx(r.Item,{name:"username",rules:[{required:!0,message:"请输入您的邮箱!"}],children:e.jsx(c,{prefix:e.jsx(j,{className:"site-form-item-icon text-blue-500"}),placeholder:"邮箱"})}),e.jsx(r.Item,{name:"password",rules:[{required:!0,message:"请输入您的密码!"}],children:e.jsx(c,{prefix:e.jsx(L,{className:"site-form-item-icon text-blue-500"}),type:"password",placeholder:"密码"})}),e.jsxs(r.Item,{children:[e.jsx(r.Item,{name:"remember",valuePropName:"checked",noStyle:!0,children:e.jsx(v,{children:"自动登录"})}),e.jsx("a",{className:"login-form-forgot float-right text-blue-500",href:"",children:"忘记密码"})]}),e.jsx(r.Item,{children:e.jsx(y,{type:"primary",htmlType:"submit",className:"w-full",children:"登录"})})]})})}),e.jsx("div",{className:"mt-8 text-center text-sm text-gray-500",children:"Copyright ©"})]})};export{K as default};