window.onload = function() {
	var modeSelect = document.getElementById("mode");
	var textarea = document.getElementById("original");
	var cm = CodeMirror(textarea);
	for (var i = 0; i < CodeMirror.modeInfo.length; i++) {
		var opt = document.createElement("option");
		opt.value = CodeMirror.modeInfo[i].name;
		opt.innerHTML = opt.value;
		modeSelect.appendChild(opt);
	}	
	function getModeInfo() {
		return CodeMirror.findModeByName(modeSelect.options[modeSelect.selectedIndex].text);
	}
	modeSelect.addEventListener("change", function() {
		CodeMirror.autoLoadMode(cm, getModeInfo().mode);
	});
	document.getElementById("highlight").addEventListener("click", function() {
		var highlighted = document.getElementById("highlighted");
		if (textarea.value == "")
			highlighted.innerHTML = "Nothing to highlight";
		else
			CodeMirror.runMode(textarea.value, getModeInfo().mime, highlighted);
	});
}
