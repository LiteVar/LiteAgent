var he=e=>{throw TypeError(e)};var N=(e,t,s)=>t.has(e)||he("Cannot "+s);var r=(e,t,s)=>(N(e,t,"read from private field"),s?s.call(e):t.get(e)),b=(e,t,s)=>t.has(e)?he("Cannot add the same private member more than once"):t instanceof WeakSet?t.add(e):t.set(e,s),u=(e,t,s,a)=>(N(e,t,"write to private field"),a?a.call(e,s):t.set(e,s),s),l=(e,t,s)=>(N(e,t,"access private method"),s);import{bD as ve,bE as ue,bF as w,bG as z,bH as P,bI as Re,bJ as $,bK as ce,bL as Oe,bM as qe,bN as we,bO as oe,bP as be,r as C,bQ as Ie,bR as Ce,bS as Ve,bT as Ee,bU as Se,bV as Ae,bW as Fe,bX as Le,bY as Te,bZ as Qe,b_ as xe,b$ as Ue,c0 as De,c1 as Me}from"./index-Bqd9ntWg.js";var m,i,B,p,F,Q,E,q,K,x,U,L,T,S,D,h,k,G,J,X,Y,Z,ee,te,pe,ye,ke=(ye=class extends ve{constructor(t,s){super();b(this,h);b(this,m);b(this,i);b(this,B);b(this,p);b(this,F);b(this,Q);b(this,E);b(this,q);b(this,K);b(this,x);b(this,U);b(this,L);b(this,T);b(this,S);b(this,D,new Set);this.options=s,u(this,m,t),u(this,q,null),u(this,E,ue()),this.options.experimental_prefetchInRender||r(this,E).reject(new Error("experimental_prefetchInRender feature flag is not enabled")),this.bindMethods(),this.setOptions(s)}bindMethods(){this.refetch=this.refetch.bind(this)}onSubscribe(){this.listeners.size===1&&(r(this,i).addObserver(this),de(r(this,i),this.options)?l(this,h,k).call(this):this.updateResult(),l(this,h,Y).call(this))}onUnsubscribe(){this.hasListeners()||this.destroy()}shouldFetchOnReconnect(){return se(r(this,i),this.options,this.options.refetchOnReconnect)}shouldFetchOnWindowFocus(){return se(r(this,i),this.options,this.options.refetchOnWindowFocus)}destroy(){this.listeners=new Set,l(this,h,Z).call(this),l(this,h,ee).call(this),r(this,i).removeObserver(this)}setOptions(t,s){const a=this.options,f=r(this,i);if(this.options=r(this,m).defaultQueryOptions(t),this.options.enabled!==void 0&&typeof this.options.enabled!="boolean"&&typeof this.options.enabled!="function"&&typeof w(this.options.enabled,r(this,i))!="boolean")throw new Error("Expected enabled to be a boolean or a callback that returns a boolean");l(this,h,te).call(this),r(this,i).setOptions(this.options),a._defaulted&&!z(this.options,a)&&r(this,m).getQueryCache().notify({type:"observerOptionsUpdated",query:r(this,i),observer:this});const c=this.hasListeners();c&&le(r(this,i),f,this.options,a)&&l(this,h,k).call(this),this.updateResult(s),c&&(r(this,i)!==f||w(this.options.enabled,r(this,i))!==w(a.enabled,r(this,i))||P(this.options.staleTime,r(this,i))!==P(a.staleTime,r(this,i)))&&l(this,h,G).call(this);const n=l(this,h,J).call(this);c&&(r(this,i)!==f||w(this.options.enabled,r(this,i))!==w(a.enabled,r(this,i))||n!==r(this,S))&&l(this,h,X).call(this,n)}getOptimisticResult(t){const s=r(this,m).getQueryCache().build(r(this,m),t),a=this.createResult(s,t);return Ke(this,a)&&(u(this,p,a),u(this,Q,this.options),u(this,F,r(this,i).state)),a}getCurrentResult(){return r(this,p)}trackResult(t,s){const a={};return Object.keys(t).forEach(f=>{Object.defineProperty(a,f,{configurable:!1,enumerable:!0,get:()=>(this.trackProp(f),s==null||s(f),t[f])})}),a}trackProp(t){r(this,D).add(t)}getCurrentQuery(){return r(this,i)}refetch({...t}={}){return this.fetch({...t})}fetchOptimistic(t){const s=r(this,m).defaultQueryOptions(t),a=r(this,m).getQueryCache().build(r(this,m),s);return a.isFetchingOptimistic=!0,a.fetch().then(()=>this.createResult(a,s))}fetch(t){return l(this,h,k).call(this,{...t,cancelRefetch:t.cancelRefetch??!0}).then(()=>(this.updateResult(),r(this,p)))}createResult(t,s){var ne;const a=r(this,i),f=this.options,c=r(this,p),n=r(this,F),v=r(this,Q),y=t!==a?t.state:r(this,B),{state:O}=t;let o={...O},A=!1,g;if(s._optimisticResults){const R=this.hasListeners(),H=!R&&de(t,s),me=R&&le(t,a,s,f);(H||me)&&(o={...o,...we(O.data,t.options)}),s._optimisticResults==="isRestoring"&&(o.fetchStatus="idle")}let{error:M,errorUpdatedAt:V,status:I}=o;if(s.select&&o.data!==void 0)if(c&&o.data===(n==null?void 0:n.data)&&s.select===r(this,K))g=r(this,x);else try{u(this,K,s.select),g=s.select(o.data),g=oe(c==null?void 0:c.data,g,s),u(this,x,g),u(this,q,null)}catch(R){u(this,q,R)}else g=o.data;if(s.placeholderData!==void 0&&g===void 0&&I==="pending"){let R;if(c!=null&&c.isPlaceholderData&&s.placeholderData===(v==null?void 0:v.placeholderData))R=c.data;else if(R=typeof s.placeholderData=="function"?s.placeholderData((ne=r(this,U))==null?void 0:ne.state.data,r(this,U)):s.placeholderData,s.select&&R!==void 0)try{R=s.select(R),u(this,q,null)}catch(H){u(this,q,H)}R!==void 0&&(I="success",g=oe(c==null?void 0:c.data,R,s),A=!0)}r(this,q)&&(M=r(this,q),g=r(this,x),V=Date.now(),I="error");const W=o.fetchStatus==="fetching",_=I==="pending",j=I==="error",ae=_&&W,ie=g!==void 0;return{status:I,fetchStatus:o.fetchStatus,isPending:_,isSuccess:I==="success",isError:j,isInitialLoading:ae,isLoading:ae,data:g,dataUpdatedAt:o.dataUpdatedAt,error:M,errorUpdatedAt:V,failureCount:o.fetchFailureCount,failureReason:o.fetchFailureReason,errorUpdateCount:o.errorUpdateCount,isFetched:o.dataUpdateCount>0||o.errorUpdateCount>0,isFetchedAfterMount:o.dataUpdateCount>y.dataUpdateCount||o.errorUpdateCount>y.errorUpdateCount,isFetching:W,isRefetching:W&&!_,isLoadingError:j&&!ie,isPaused:o.fetchStatus==="paused",isPlaceholderData:A,isRefetchError:j&&ie,isStale:re(t,s),refetch:this.refetch,promise:r(this,E)}}updateResult(t){const s=r(this,p),a=this.createResult(r(this,i),this.options);if(u(this,F,r(this,i).state),u(this,Q,this.options),r(this,F).data!==void 0&&u(this,U,r(this,i)),z(a,s))return;if(this.options.experimental_prefetchInRender){const n=y=>{a.status==="error"?y.reject(a.error):a.data!==void 0&&y.resolve(a.data)},v=()=>{const y=u(this,E,a.promise=ue());n(y)},d=r(this,E);switch(d.status){case"pending":n(d);break;case"fulfilled":(a.status==="error"||a.data!==d.value)&&v();break;case"rejected":(a.status!=="error"||a.error!==d.reason)&&v();break}}u(this,p,a);const f={},c=()=>{if(!s)return!0;const{notifyOnChangeProps:n}=this.options,v=typeof n=="function"?n():n;if(v==="all"||!v&&!r(this,D).size)return!0;const d=new Set(v??r(this,D));return this.options.throwOnError&&d.add("error"),Object.keys(r(this,p)).some(y=>{const O=y;return r(this,p)[O]!==s[O]&&d.has(O)})};(t==null?void 0:t.listeners)!==!1&&c()&&(f.listeners=!0),l(this,h,pe).call(this,{...f,...t})}onQueryUpdate(){this.updateResult(),this.hasListeners()&&l(this,h,Y).call(this)}},m=new WeakMap,i=new WeakMap,B=new WeakMap,p=new WeakMap,F=new WeakMap,Q=new WeakMap,E=new WeakMap,q=new WeakMap,K=new WeakMap,x=new WeakMap,U=new WeakMap,L=new WeakMap,T=new WeakMap,S=new WeakMap,D=new WeakMap,h=new WeakSet,k=function(t){l(this,h,te).call(this);let s=r(this,i).fetch(this.options,t);return t!=null&&t.throwOnError||(s=s.catch(Re)),s},G=function(){l(this,h,Z).call(this);const t=P(this.options.staleTime,r(this,i));if($||r(this,p).isStale||!ce(t))return;const a=Oe(r(this,p).dataUpdatedAt,t)+1;u(this,L,setTimeout(()=>{r(this,p).isStale||this.updateResult()},a))},J=function(){return(typeof this.options.refetchInterval=="function"?this.options.refetchInterval(r(this,i)):this.options.refetchInterval)??!1},X=function(t){l(this,h,ee).call(this),u(this,S,t),!($||w(this.options.enabled,r(this,i))===!1||!ce(r(this,S))||r(this,S)===0)&&u(this,T,setInterval(()=>{(this.options.refetchIntervalInBackground||qe.isFocused())&&l(this,h,k).call(this)},r(this,S)))},Y=function(){l(this,h,G).call(this),l(this,h,X).call(this,l(this,h,J).call(this))},Z=function(){r(this,L)&&(clearTimeout(r(this,L)),u(this,L,void 0))},ee=function(){r(this,T)&&(clearInterval(r(this,T)),u(this,T,void 0))},te=function(){const t=r(this,m).getQueryCache().build(r(this,m),this.options);if(t===r(this,i))return;const s=r(this,i);u(this,i,t),u(this,B,t.state),this.hasListeners()&&(s==null||s.removeObserver(this),t.addObserver(this))},pe=function(t){be.batch(()=>{t.listeners&&this.listeners.forEach(s=>{s(r(this,p))}),r(this,m).getQueryCache().notify({query:r(this,i),type:"observerResultsUpdated"})})},ye);function Be(e,t){return w(t.enabled,e)!==!1&&e.state.data===void 0&&!(e.state.status==="error"&&t.retryOnMount===!1)}function de(e,t){return Be(e,t)||e.state.data!==void 0&&se(e,t,t.refetchOnMount)}function se(e,t,s){if(w(t.enabled,e)!==!1){const a=typeof s=="function"?s(e):s;return a==="always"||a!==!1&&re(e,t)}return!1}function le(e,t,s,a){return(e!==t||w(a.enabled,e)===!1)&&(!s.suspense||e.state.status!=="error")&&re(e,s)}function re(e,t){return w(t.enabled,e)!==!1&&e.isStaleByTime(P(t.staleTime,e))}function Ke(e,t){return!z(e.getCurrentResult(),t)}var ge=C.createContext(!1),Pe=()=>C.useContext(ge);ge.Provider;function We(){let e=!1;return{clearReset:()=>{e=!1},reset:()=>{e=!0},isReset:()=>e}}var _e=C.createContext(We()),je=()=>C.useContext(_e);function He(e,t){return typeof e=="function"?e(...t):!!e}function Ne(){}var ze=(e,t)=>{(e.suspense||e.throwOnError)&&(t.isReset()||(e.retryOnMount=!1))},$e=e=>{C.useEffect(()=>{e.clearReset()},[e])},Ge=({result:e,errorResetBoundary:t,throwOnError:s,query:a})=>e.isError&&!t.isReset()&&!e.isFetching&&a&&He(s,[e.error,a]),Je=e=>{e.suspense&&(typeof e.staleTime!="number"&&(e.staleTime=1e3),typeof e.gcTime=="number"&&(e.gcTime=Math.max(e.gcTime,1e3)))},Xe=(e,t)=>e.isLoading&&e.isFetching&&!t,Ye=(e,t)=>(e==null?void 0:e.suspense)&&t.isPending,fe=(e,t,s)=>t.fetchOptimistic(e).catch(()=>{s.clearReset()});function Ze(e,t,s){var O,o,A,g,M;const a=Ie(),f=Pe(),c=je(),n=a.defaultQueryOptions(e);(o=(O=a.getDefaultOptions().queries)==null?void 0:O._experimental_beforeQuery)==null||o.call(O,n),n._optimisticResults=f?"isRestoring":"optimistic",Je(n),ze(n,c),$e(c);const v=!a.getQueryCache().get(n.queryHash),[d]=C.useState(()=>new t(a,n)),y=d.getOptimisticResult(n);if(C.useSyncExternalStore(C.useCallback(V=>{const I=f?()=>{}:d.subscribe(be.batchCalls(V));return d.updateResult(),I},[d,f]),()=>d.getCurrentResult(),()=>d.getCurrentResult()),C.useEffect(()=>{d.setOptions(n,{listeners:!1})},[n,d]),Ye(n,y))throw fe(n,d,c);if(Ge({result:y,errorResetBoundary:c,throwOnError:n.throwOnError,query:a.getQueryCache().get(n.queryHash)}))throw y.error;if((g=(A=a.getDefaultOptions().queries)==null?void 0:A._experimental_afterQuery)==null||g.call(A,n,y),n.experimental_prefetchInRender&&!$&&Xe(y,f)){const V=v?fe(n,d,c):(M=a.getQueryCache().get(n.queryHash))==null?void 0:M.promise;V==null||V.catch(Ne).finally(()=>{d.hasListeners()||d.updateResult()})}return n.notifyOnChangeProps?y:d.trackResult(y)}function rt(e,t){return Ze(e,ke)}const at=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await Ce({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1AuthInitStatus",params:{body:e==null?void 0:e.body,headers:e==null?void 0:e.headers,path:e==null?void 0:e.path,query:e==null?void 0:e.query}}]}),it=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await Ve({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1ToolList",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]}),nt=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await Ee({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1ToolDetailById",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]}),ht=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await Se({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1ModelList",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]}),ut=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await Ae({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1WorkspaceList",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]}),ct=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await Fe({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1WorkspaceMemberList",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]}),ot=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await Le({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1WorkspaceActivateInfo",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]}),dt=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await Te({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1AgentList",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]}),lt=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await Qe({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1AgentAdminList",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]}),ft=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await xe({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1AgentById",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]}),yt=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await Ue({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1AgentAdminInfoById",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]}),bt=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await De({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1ChatRecentAgent",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]}),pt=e=>({queryFn:async({queryKey:t})=>{const{data:s}=await Me({...e,...t[0].params,throwOnError:!0});return s},queryKey:[{scope:"getV1UserInfo",params:{body:e.body,headers:e.headers,path:e.path,query:e.query}}]});export{ut as a,dt as b,ft as c,pt as d,bt as e,ot as f,at as g,yt as h,it as i,ht as j,lt as k,nt as l,ct as m,rt as u};