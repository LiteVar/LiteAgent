<?xml version='1.0' encoding='UTF-8'?>
<Project Type="Project" LVVersion="17008000">
	<Item Name="我的电脑" Type="My Computer">
		<Property Name="server.app.propertiesEnabled" Type="Bool">true</Property>
		<Property Name="server.control.propertiesEnabled" Type="Bool">true</Property>
		<Property Name="server.tcp.enabled" Type="Bool">false</Property>
		<Property Name="server.tcp.port" Type="Int">0</Property>
		<Property Name="server.tcp.serviceName" Type="Str">我的电脑/VI服务器</Property>
		<Property Name="server.tcp.serviceName.default" Type="Str">我的电脑/VI服务器</Property>
		<Property Name="server.vi.callsEnabled" Type="Bool">true</Property>
		<Property Name="server.vi.propertiesEnabled" Type="Bool">true</Property>
		<Property Name="specify.custom.address" Type="Bool">false</Property>
		<Item Name="Bin" Type="Folder">
			<Item Name="LaunchDarkly.EventSource.dll" Type="Document" URL="../Source/Bin/LaunchDarkly.EventSource.dll"/>
			<Item Name="LaunchDarkly.Logging.dll" Type="Document" URL="../Source/Bin/LaunchDarkly.Logging.dll"/>
			<Item Name="LiteAgentSDK.DotNet.dll" Type="Document" URL="../Source/Bin/LiteAgentSDK.DotNet.dll"/>
			<Item Name="LiteAgentSDK.LabVIEW.dll" Type="Document" URL="../Source/Bin/LiteAgentSDK.LabVIEW.dll"/>
			<Item Name="Newtonsoft.Json.dll" Type="Document" URL="../Source/Bin/Newtonsoft.Json.dll"/>
			<Item Name="Refit.dll" Type="Document" URL="../Source/Bin/Refit.dll"/>
			<Item Name="Refit.Newtonsoft.Json.dll" Type="Document" URL="../Source/Bin/Refit.Newtonsoft.Json.dll"/>
			<Item Name="System.Buffers.dll" Type="Document" URL="../Source/Bin/System.Buffers.dll"/>
			<Item Name="System.Memory.dll" Type="Document" URL="../Source/Bin/System.Memory.dll"/>
			<Item Name="System.Runtime.CompilerServices.Unsafe.dll" Type="Document" URL="../Source/Bin/System.Runtime.CompilerServices.Unsafe.dll"/>
			<Item Name="System.Text.Json.dll" Type="Document" URL="../Source/Bin/System.Text.Json.dll"/>
			<Item Name="System.Threading.Tasks.Extensions.dll" Type="Document" URL="../Source/Bin/System.Threading.Tasks.Extensions.dll"/>
		</Item>
		<Item Name="Example" Type="Folder">
			<Item Name="Example.vi" Type="VI" URL="../Example/Example.vi"/>
			<Item Name="Execute.vi" Type="VI" URL="../Example/Execute.vi"/>
			<Item Name="StringArrayToMultiLineString.vi" Type="VI" URL="../Example/StringArrayToMultiLineString.vi"/>
		</Item>
		<Item Name="LiteAgentSDK.lvlib" Type="Library" URL="../LiteAgentSDK.lvlib"/>
		<Item Name="LiteAgentSDK.vipb" Type="Document" URL="../LiteAgentSDK.vipb"/>
		<Item Name="依赖关系" Type="Dependencies">
			<Item Name="vi.lib" Type="Folder">
				<Item Name="Clear Errors.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/Clear Errors.vi"/>
				<Item Name="i3-json.lvlib" Type="Library" URL="/&lt;vilib&gt;/LVH/i3 JSON/i3-json.lvlib"/>
				<Item Name="Trim Whitespace.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/Trim Whitespace.vi"/>
				<Item Name="UTF8 Tools.lvlib" Type="Library" URL="/&lt;vilib&gt;/addons/Tools/Unicode/UTF8 Tools.lvlib"/>
				<Item Name="whitespace.ctl" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/whitespace.ctl"/>
			</Item>
			<Item Name="mscorlib" Type="VI" URL="mscorlib">
				<Property Name="NI.PreserveRelativePath" Type="Bool">true</Property>
			</Item>
		</Item>
		<Item Name="程序生成规范" Type="Build"/>
	</Item>
</Project>
