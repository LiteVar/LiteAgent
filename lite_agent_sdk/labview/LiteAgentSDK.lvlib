<?xml version='1.0' encoding='UTF-8'?>
<Library LVVersion="17008000">
	<Property Name="NI.Lib.Icon" Type="Bin">&amp;Q#!!!!!!!)!"1!&amp;!!!-!%!!!@````]!!!!"!!%!!!*G!!!*Q(C=\&gt;5RDB."%)8B"S)A)0%.E&amp;0#CMB^"2]!L62%#"%ZG!1273)ALI!,_!I_A*/[AH&amp;!\AO!'0\OK&lt;5N1/O%&amp;11\M_U&gt;PZ[O`NT4[Z8[]62[IP(;M&lt;PT'%=+N9PTS_\SCP(H](SJ8`N\U%N.6\N_*:X'X^;``:H[&gt;\`X6Z(JPD`U8^22_&lt;F1!;;B5W=\\`L].@_6^&lt;FS[/&amp;Y/0\N]@\K*LXX0@OI`;70N+1&amp;T7GG.N79Z%G?Z%G?Z%E?Z%%?Z%%?Z%(OZ%\OZ%\OZ%ZOZ%:OZ%:OZ%&lt;?4X+2CVTEE*,*EYG3IEG"Z'9I3FY34_**0)G(NUI]C3@R**\%QSV+0)EH]33?R--Q*:\%EXA34_+B6*&gt;E0]HR*"\++`!%HM!4?!)05SLQ")"AMK"Q5!3'AM\A)P!%HM$$J1*0Y!E]A3@QU+X!%XA#4_!*0!TJKR*&gt;UUZS0*32YX%]DM@R/"Z+S`%Y(M@D?"Q0U]HR/"Y(Y5TI&amp;)=A:Z"TA`0']4A?@MHR/"\(YXA=$VX^#8F@G;:J*TE?QW.Y$)`B-4S5E/%R0)&lt;(]"A?SMLQ'"\$9XA-$V0*]"A?QW.!D%G:8E9R9["RER%9(H\[&lt;L(_F+*,L*^3&lt;6\6JF2N.N5G5GU/V5.805T61V)NPGJ269OF7A46BV/B62D6*+L"\59&gt;?&gt;X4NL1.&lt;5V&lt;U:;U"7V/G\7B@`H'Y`'I`8[P\8;LT7;D^8KNV7KFZ8+JR7+B_8SOW7RW_BJYTHH[1DB`,XU;@IS(&lt;^`(,_]_PTI-&lt;WY/Q\/XBW&amp;Y`?(FRR&gt;U@\X(`[8`Q8?D(OOS,GPU%XMD\7E!!!!!</Property>
	<Property Name="NI.Lib.SourceVersion" Type="Int">385908736</Property>
	<Property Name="NI.Lib.Version" Type="Str">1.0.0.0</Property>
	<Property Name="NI.LV.All.SourceOnly" Type="Bool">false</Property>
	<Item Name="Handlers" Type="Folder">
		<Item Name="DequeueEvent.vi" Type="VI" URL="../Source/Private/DequeueEvent.vi"/>
		<Item Name="HandleEvent.vi" Type="VI" URL="../Source/Private/HandleEvent.vi"/>
		<Item Name="InitHandler.vi" Type="VI" URL="../Source/Private/InitHandler.vi"/>
		<Item Name="OnChunk.vi" Type="VI" URL="../Source/Private/OnChunk.vi"/>
		<Item Name="OnDone.vi" Type="VI" URL="../Source/Private/OnDone.vi"/>
		<Item Name="OnError.vi" Type="VI" URL="../Source/Private/OnError.vi"/>
		<Item Name="OnEvent.vi" Type="VI" URL="../Source/Private/OnEvent.vi"/>
		<Item Name="OnFunctionCall.vi" Type="VI" URL="../Source/Private/OnFunctionCall.vi"/>
		<Item Name="OnMessage.vi" Type="VI" URL="../Source/Private/OnMessage.vi"/>
		<Item Name="ParseEventJson.vi" Type="VI" URL="../Source/Private/ParseEventJson.vi"/>
	</Item>
	<Item Name="Models" Type="Folder">
		<Item Name="AgentMessage.ctl" Type="VI" URL="../Source/AgentMessage.ctl"/>
		<Item Name="AgentMessageChunk.ctl" Type="VI" URL="../Source/AgentMessageChunk.ctl"/>
		<Item Name="Completions.ctl" Type="VI" URL="../Source/Completions.ctl"/>
		<Item Name="Content.ctl" Type="VI" URL="../Source/Content.ctl"/>
		<Item Name="ContentList.ctl" Type="VI" URL="../Source/ContentList.ctl"/>
		<Item Name="Dispatch.ctl" Type="VI" URL="../Source/Dispatch.ctl"/>
		<Item Name="FunctionCall.ctl" Type="VI" URL="../Source/FunctionCall.ctl"/>
		<Item Name="LiteAgent.ctl" Type="VI" URL="../Source/LiteAgent.ctl"/>
		<Item Name="Reflection.ctl" Type="VI" URL="../Source/Reflection.ctl"/>
		<Item Name="Session.ctl" Type="VI" URL="../Source/Private/Session.ctl"/>
		<Item Name="TaskStatus.ctl" Type="VI" URL="../Source/TaskStatus.ctl"/>
		<Item Name="ToolCalls.ctl" Type="VI" URL="../Source/ToolCalls.ctl"/>
		<Item Name="ToolReturn.ctl" Type="VI" URL="../Source/ToolReturn.ctl"/>
		<Item Name="UserTask.ctl" Type="VI" URL="../Source/UserTask.ctl"/>
		<Item Name="VersionModel.ctl" Type="VI" URL="../Source/Private/VersionModel.ctl"/>
	</Item>
	<Item Name="Private" Type="Folder">
		<Item Name="Callback.vi" Type="VI" URL="../Source/Private/Callback.vi"/>
		<Item Name="Chat.vi" Type="VI" URL="../Source/Private/Chat.vi"/>
		<Item Name="Clear.vi" Type="VI" URL="../Source/Private/Clear.vi"/>
		<Item Name="Context.vi" Type="VI" URL="../Source/Private/Context.vi"/>
		<Item Name="GetHistory.vi" Type="VI" URL="../Source/Private/GetHistory.vi"/>
		<Item Name="GetVersion.vi" Type="VI" URL="../Source/Private/GetVersion.vi"/>
		<Item Name="Init.vi" Type="VI" URL="../Source/Private/Init.vi"/>
		<Item Name="InitSession.vi" Type="VI" URL="../Source/Private/InitSession.vi"/>
		<Item Name="Release.vi" Type="VI" URL="../Source/Private/Release.vi"/>
		<Item Name="Stop.vi" Type="VI" URL="../Source/Private/Stop.vi"/>
	</Item>
	<Item Name="LACallback.vi" Type="VI" URL="../Source/LACallback.vi"/>
	<Item Name="LAChat.vi" Type="VI" URL="../Source/LAChat.vi"/>
	<Item Name="LAChatByText.vi" Type="VI" URL="../Source/LAChatByText.vi"/>
	<Item Name="LAChatByUserTask.vi" Type="VI" URL="../Source/LAChatByUserTask.vi"/>
	<Item Name="LAClear.vi" Type="VI" URL="../Source/LAClear.vi"/>
	<Item Name="LAGetHistory.vi" Type="VI" URL="../Source/LAGetHistory.vi"/>
	<Item Name="LAInit.vi" Type="VI" URL="../Source/LAInit.vi"/>
	<Item Name="LAInitSession.vi" Type="VI" URL="../Source/LAInitSession.vi"/>
	<Item Name="LAListen.vi" Type="VI" URL="../Source/LAListen.vi"/>
	<Item Name="LAOnChunk.vi" Type="VI" URL="../Source/LAOnChunk.vi"/>
	<Item Name="LAOnDone.vi" Type="VI" URL="../Source/LAOnDone.vi"/>
	<Item Name="LAOnError.vi" Type="VI" URL="../Source/LAOnError.vi"/>
	<Item Name="LAOnEvent.vi" Type="VI" URL="../Source/LAOnEvent.vi"/>
	<Item Name="LAOnFunctionCall.vi" Type="VI" URL="../Source/LAOnFunctionCall.vi"/>
	<Item Name="LAOnMessage.vi" Type="VI" URL="../Source/LAOnMessage.vi"/>
	<Item Name="LARelease.vi" Type="VI" URL="../Source/LARelease.vi"/>
	<Item Name="LAStop.vi" Type="VI" URL="../Source/LAStop.vi"/>
	<Item Name="ParseAgentMessage.vi" Type="VI" URL="../Source/ParseAgentMessage.vi"/>
	<Item Name="ParseAgentMessageChunk.vi" Type="VI" URL="../Source/ParseAgentMessageChunk.vi"/>
</Library>
