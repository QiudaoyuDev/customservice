(function () {
  const STORAGE_KEY = "hardware-ai-pilot-prototype-v1";
  const defaultState = {
    view: "brief",
    language: "zh",
    supportStage: "identify",
    path: [],
    handoffStatus: "new",
    resolution: "hardware_failure",
    resolutionNote: "",
    knowledgeSignal: false,
  };

  let state = loadState();
  let toastTimer;
  const app = document.getElementById("app");
  const toast = document.getElementById("toast");

  function loadState() {
    try {
      return { ...defaultState, ...JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}") };
    } catch {
      return { ...defaultState };
    }
  }

  function saveState() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }

  function setState(patch, message) {
    state = { ...state, ...patch };
    saveState();
    render();
    if (message) showToast(message);
  }

  function showToast(message) {
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.classList.add("show");
    toastTimer = setTimeout(() => toast.classList.remove("show"), 2600);
  }

  function t(zh, en) {
    return state.language === "zh" ? zh : en;
  }

  function currentJourneyIndex() {
    if (state.view === "brief") return 0;
    if (state.view === "support") return state.supportStage === "identify" ? 1 : 2;
    if (state.view === "handoff") return 3;
    return 4;
  }

  function updateChrome() {
    document.querySelectorAll(".view-switch button").forEach((button) => {
      button.classList.toggle("active", button.dataset.view === state.view);
    });
    const current = currentJourneyIndex();
    document.querySelectorAll(".journey-bar button").forEach((button, index) => {
      button.classList.toggle("done", index + 1 < current || current === 4);
      button.classList.toggle("current", index + 1 === current);
    });
  }

  function render() {
    updateChrome();
    if (state.view === "brief") app.innerHTML = renderBrief();
    if (state.view === "support") app.innerHTML = renderSupport();
    if (state.view === "handoff") app.innerHTML = renderHandoff();
    if (state.view === "insights") app.innerHTML = renderInsights();
    requestAnimationFrame(() => {
      const phoneBody = document.querySelector(".phone-body");
      if (phoneBody) phoneBody.scrollTop = phoneBody.scrollHeight;
    });
  }

  function renderBrief() {
    return `
      <section class="page">
        <div class="brief-hero">
          <div class="eyebrow">Pilot validation · v1</div>
          <h1>先验证一条可信的售后闭环，<br />再扩展成通用客服平台。</h1>
          <p class="lead">本原型围绕 X100 机顶盒的网络故障与过热风险，演示设备上下文、确定性诊断、安全停机、人工交接和知识回流。它刻意不演示泛聊天能力。</p>
          <div class="hero-actions">
            <button class="button ai" data-action="start-demo">开始 5 分钟闭环演示</button>
            <button class="button quiet" data-action="go" data-view="insights">查看试点决策指标</button>
          </div>
        </div>
        <div class="brief-grid">
          ${briefCard("01", "识别是否准确", "扫码后直接锁定型号、硬件版本、固件与地区，避免跨型号误答。")}
          ${briefCard("02", "诊断是否受控", "AI 解释和引导，但每个高频故障按已审核状态机推进，不自由编造步骤。")}
          ${briefCard("03", "风险能否拦截", "出现烧焦、破损、触电等信号时强制停止自助，生成不可变交接包。")}
          ${briefCard("04", "数据能否回流", "结案原因沉淀为知识缺口、流程优化或产品质量信号，而不是直接喂回模型。")}
        </div>
        <div class="scope-strip card">
          <strong>本轮原型边界</strong>
          <span>一家试点厂商 · 一个产品系列 · 两个代表性故障 · 中英双语 · 内部人工队列。暂不验证退款、远控、全渠道客服与自动发布知识。</span>
        </div>
      </section>`;
  }

  function briefCard(num, title, text) {
    return `<article class="brief-card card"><span class="num">${num}</span><h3>${title}</h3><p>${text}</p></article>`;
  }

  function renderSupport() {
    const path = state.path;
    const identifiedOnly = state.supportStage === "identify";
    const hasIssue = path.includes("wifi");
    const canSeeNetworks = path.includes("networks-yes");
    const restarted = path.includes("restart-no");
    const safety = path.includes("heat-yes");
    return `
      <section class="page support-page">
        <div class="support-layout">
          <div class="phone-stage">
            <div class="phone" aria-label="终端用户移动端原型">
              <div class="phone-status"><span>09:41</span><span>5G · 86%</span></div>
              <header class="phone-header">
                <span class="device-icon">X</span>
                <div><strong>X100 Android TV Box</strong><small>EU · rev.B · FW 2.1.0</small></div>
                <div class="lang-toggle" aria-label="语言">
                  <button class="${state.language === "en" ? "active" : ""}" data-action="lang" data-lang="en">EN</button>
                  <button class="${state.language === "zh" ? "active" : ""}" data-action="lang" data-lang="zh">中</button>
                </div>
              </header>
              <div class="phone-body">
                ${identifiedOnly ? renderIdentified() : `
                  <div class="chat-stack">
                    <div class="identified">
                      <div class="identified-top"><span class="signal"></span>${t("已识别设备与适用知识范围", "Device and applicable knowledge identified")}</div>
                    </div>
                    ${!hasIssue ? renderIssueChoice() : ""}
                    ${hasIssue ? `<div class="bubble user-message">${t("Wi-Fi 能搜到，但一直连不上。", "I can see Wi-Fi networks, but it will not connect.")}</div>${renderFirstStep(canSeeNetworks)}` : ""}
                    ${canSeeNetworks ? `<div class="bubble user-message">${t("能看到其它网络。", "Yes, I can see other networks.")}</div>${renderRestartStep(restarted)}` : ""}
                    ${restarted ? `<div class="bubble user-message">${t("重启后还是不行。", "It still fails after restarting.")}</div>${renderSafetyCheck(safety)}` : ""}
                    ${safety ? renderSafetyResult() : ""}
                  </div>`}
              </div>
              <footer class="phone-composer">
                <div><input value="" placeholder="${t("描述问题或输入错误码…", "Describe the problem or enter an error code…")}" disabled /><button class="button ai small" disabled>${t("发送", "Send")}</button></div>
                <small>${t("图片仅用于错误码、接口和标签识别。", "Images are only used to identify error codes, ports and labels.")}</small>
              </footer>
            </div>
          </div>
          <aside class="context-panel card">
            <div class="panel-head"><h3>诊断状态机</h3><p>用户看到清晰步骤，系统保留可审计状态。</p></div>
            <div class="rail">
              ${railStep(1, "设备识别", "QR → X100 / EU", "done")}
              ${railStep(2, "网络可见性", "wifi-check-01", canSeeNetworks ? "done" : hasIssue ? "current" : "pending")}
              ${railStep(3, "重启与复测", "wifi-check-02", restarted ? "done" : canSeeNetworks ? "current" : "pending")}
              ${railStep("!", "过热风险检查", "safety-stop", safety ? "risk-done" : restarted ? "risk-current" : "pending")}
              ${railStep(4, "人工交接", "HAND-240817-38", safety ? "current" : "pending")}
            </div>
            <div class="truth-box">原型规则：AI 不能跳过高风险节点，不能自行修改已发布流程；知识不足、版本冲突或安全命中都必须降级。</div>
          </aside>
        </div>
      </section>`;
  }

  function renderIdentified() {
    return `
      <div class="identified">
        <div class="identified-top"><span class="signal"></span>${t("设备识别成功", "Device identified")}</div>
        <h3 style="margin-top:12px">X100 Android TV Box</h3>
        <div class="device-facts">
          <div><small>${t("硬件", "Hardware")}</small><b>rev.B</b></div>
          <div><small>${t("固件", "Firmware")}</small><b>2.1.0</b></div>
          <div><small>${t("地区", "Region")}</small><b>EU</b></div>
          <div><small>${t("保修", "Warranty")}</small><b>${t("有效", "Active")}</b></div>
        </div>
        <p class="muted" style="font-size:11px">${t("接下来的回答只会使用适用于该版本和地区的已发布知识。", "Answers will only use published knowledge applicable to this version and region.")}</p>
        <button class="button ai wide" data-action="begin-diagnosis">${t("开始设备诊断", "Start device diagnosis")}</button>
      </div>`;
  }

  function renderIssueChoice() {
    return `<div class="bubble ai-message"><div class="speaker">AI DIAGNOSIS</div>${t("请选择最接近的问题。", "Choose the closest issue.")}<div class="choice-grid"><button data-action="choose-wifi">${t("Wi-Fi 无法连接", "Wi-Fi will not connect")}</button><button data-action="toast" data-message="该路径将在下一轮原型补充">${t("遥控器没有反应", "Remote is not responding")}</button><button data-action="toast" data-message="该路径将在下一轮原型补充">${t("电视没有画面", "No picture on TV")}</button></div></div>`;
  }

  function renderFirstStep(done) {
    if (done) return "";
    return `<div class="bubble ai-message"><div class="speaker">AI DIAGNOSIS · STEP 1/3</div>${t("先确认设备是否能发现无线网络。", "First, confirm whether the device can discover wireless networks.")}<div class="step-card"><div class="step-label">DECISION · wifi-check-01</div><p>${t("设置页中能看到其它 Wi-Fi 名称吗？", "Can you see other Wi-Fi names in Settings?")}</p><div class="step-actions"><button class="button ai" data-action="networks-yes">${t("可以", "Yes")}</button><button class="button" data-action="toast" data-message="已记录：无法发现网络，将进入天线/频段分支">${t("不可以", "No")}</button></div></div><div class="citation">MANUAL-X100-EU §3.2 · revision 6</div></div>`;
  }

  function renderRestartStep(done) {
    if (done) return "";
    return `<div class="bubble ai-message"><div class="speaker">AI DIAGNOSIS · STEP 2/3</div><div class="step-card"><div class="step-label">OPERATION · wifi-check-02</div><p>${t("关闭机顶盒和路由器电源，等待 30 秒后重新开启。", "Turn off the TV box and router, wait 30 seconds, then turn them on again.")}</p><div class="step-actions"><button class="button" data-action="toast" data-message="演示路径已记录为解决；可重置后重新体验">${t("已解决", "Resolved")}</button><button class="button ai" data-action="restart-no">${t("仍未解决", "Still failing")}</button></div></div><div class="citation">KB-NET-102 · published v3</div></div>`;
  }

  function renderSafetyCheck(done) {
    if (done) return "";
    return `<div class="bubble ai-message"><div class="speaker">SAFETY GATE · REQUIRED</div>${t("继续前必须确认安全状况。", "A safety check is required before continuing.")}<div class="step-card"><div class="step-label">HIGH RISK · thermal-check</div><p>${t("设备或电源接口是否异常发热、有烧焦气味或可见损坏？", "Is the device or power port unusually hot, burnt-smelling, or visibly damaged?")}</p><div class="step-actions"><button class="button safety" data-action="heat-yes">${t("是，有烧焦气味", "Yes, there is a burnt smell")}</button><button class="button" data-action="toast" data-message="未命中安全风险，将进入网络重置分支">${t("没有", "No")}</button></div></div></div>`;
  }

  function renderSafetyResult() {
    return `<div class="safety-card"><strong>${t("安全停止 · 请勿继续通电", "Safety stop · Do not power on again")}</strong>${t("已停止自动诊断。请断开电源并保持设备原状；系统已生成交接包，人工客服将看到型号、版本、已尝试步骤与风险描述。", "Automated diagnosis has stopped. Disconnect power and leave the device as-is. A handoff package now includes the model, version, attempted steps and risk description.")}<button class="button human wide" style="margin-top:12px" data-action="open-handoff">${t("查看人工交接", "View human handoff")}</button></div>`;
  }

  function railStep(index, title, code, status) {
    const done = status === "done" || status === "risk-done";
    const current = status === "current" || status === "risk-current";
    const risk = status === "risk-current" || status === "risk-done";
    const classes = `${done ? "done" : ""} ${current ? "current" : ""} ${risk ? "risk" : ""}`;
    return `<div class="rail-step ${classes}"><i>${done ? "✓" : index}</i><div><strong>${title}</strong><small>${code}</small></div></div>`;
  }

  function renderHandoff() {
    const claimed = state.handoffStatus === "claimed";
    const closed = state.handoffStatus === "closed";
    return `
      <section class="page console-page">
        <div class="console-shell card">
          ${renderSidebar("handoff")}
          <div class="console-main">
            <header class="console-top"><span class="muted">人工协同 › <b style="color:var(--ink)">接管队列</b></span><span class="health"><i></i>内部队列正常</span></header>
            <div class="console-content">
              <div class="page-title"><div><div class="eyebrow">Human handoff</div><h2>人工接管工作台</h2><p>AI 已收集信息，人工负责判断、承诺与结案。</p></div>${closed ? `<button class="button ai" data-action="go" data-view="insights">查看知识回流</button>` : ""}</div>
              <div class="kpi-row">
                ${kpi("待接管", closed ? "2" : "3", "高风险 1")}${kpi("处理中", claimed ? "6" : "5", "SLA 内 100%")}${kpi("今日结案", closed ? "29" : "28", "较昨日 +8%")}${kpi("无需复述", "86%", "交接包完整")}
              </div>
              <div class="workbench card">
                <section class="queue">
                  <div class="queue-head">优先队列 <span class="tag safety" style="float:right">P0 1</span></div>
                  <button class="queue-item active"><strong><span>X100 · 过热风险</span><span class="tag ${closed ? "ok" : "safety"}">${closed ? "已结案" : "P0"}</span></strong><p>烧焦气味 / Wi-Fi 故障 · EU</p><small>HAND-240817-38 · 2 分钟前</small></button>
                  <button class="queue-item" data-action="toast" data-message="已切换到遥控器工单（本原型不展开该路径）"><strong><span>X100 · 遥控器</span><span class="tag human">P2</span></strong><p>按键无响应 · UK</p><small>HAND-240817-37 · 8 分钟前</small></button>
                </section>
                <section class="case-thread">
                  <div class="case-head">HAND-240817-38 <span class="tag ${closed ? "ok" : claimed ? "human" : "safety"}" style="float:right">${closed ? "CLOSED" : claimed ? "IN PROGRESS" : "NEW"}</span></div>
                  <div class="case-body">
                    <div class="case-summary"><b>系统摘要</b><p>用户反馈 Wi-Fi 无法连接；重启设备与路由器未解决。安全检查中确认电源接口有烧焦气味，已强制停止通电。</p></div>
                    ${timeline("09:38", "二维码识别 X100 / rev.B / FW 2.1.0 / EU")}
                    ${timeline("09:39", "完成网络可见性检查：可发现其它 SSID")}
                    ${timeline("09:40", "执行重启与复测：未解决")}
                    ${timeline("09:41", "命中 HIGH 风险：烧焦气味；AI 停止自动诊断")}
                    ${closed ? timeline("09:45", "人工确认硬件故障，安排保修换机并创建产品质量信号") : ""}
                  </div>
                  <div class="case-actions">
                    ${!claimed && !closed ? `<button class="button human" data-action="claim">接管工单</button>` : ""}
                    ${claimed ? `<button class="button primary" data-action="close-case">确认结案并回流</button>` : ""}
                    ${closed ? `<button class="button" data-action="go" data-view="support">查看用户侧记录</button>` : ""}
                  </div>
                </section>
                <aside class="copilot">
                  <h3>交接包</h3><p class="muted" style="font-size:11px">不可变快照 · 生成于 09:41</p>
                  <div class="copilot-card"><label>设备上下文</label><div class="mono" style="font-size:10px;line-height:1.8">X100 · rev.B<br />FW 2.1.0 · EU<br />QR: 7f3…a91</div></div>
                  <div class="copilot-card"><label>证据与附件</label><span class="tag ai">KB-NET-102 v3</span> <span class="tag safety">高风险命中</span></div>
                  <div class="copilot-card"><label for="resolution">结案原因</label><select id="resolution" data-action="resolution"><option value="hardware_failure" ${state.resolution === "hardware_failure" ? "selected" : ""}>硬件故障 / 换机</option><option value="knowledge_gap" ${state.resolution === "knowledge_gap" ? "selected" : ""}>知识缺口</option><option value="flow_gap" ${state.resolution === "flow_gap" ? "selected" : ""}>流程缺口</option></select></div>
                  <div class="copilot-card"><label for="resolution-note">人工备注</label><textarea id="resolution-note" data-action="resolution-note" placeholder="记录检查与承诺…">${escapeHtml(state.resolutionNote)}</textarea></div>
                </aside>
              </div>
            </div>
          </div>
        </div>
      </section>`;
  }

  function renderSidebar(active) {
    return `<aside class="console-sidebar"><div class="console-logo">HARDWARE AI<small>SUPPORT OPERATIONS</small></div><div class="nav-label">OPERATIONS</div><nav class="console-nav"><button data-action="go" data-view="insights">总览与洞察</button><button data-action="toast" data-message="产品与版本管理沿用现有实现，本轮原型不展开">产品与版本</button><button data-action="toast" data-message="知识中心将在下一轮原型展开审核与发布链路">知识中心</button><button data-action="toast" data-message="诊断流程编辑器沿用现有实现，本轮聚焦运行闭环">诊断流程</button></nav><div class="nav-label">SERVICE</div><nav class="console-nav"><button class="${active === "handoff" ? "active" : ""}" data-action="go" data-view="handoff">人工接管</button><button class="${active === "insights" ? "active" : ""}" data-action="go" data-view="insights">质量信号</button></nav></aside>`;
  }

  function kpi(label, value, detail) {
    return `<div class="kpi card"><span>${label}</span><strong>${value}</strong><small>${detail}</small></div>`;
  }

  function timeline(time, text) {
    return `<div class="timeline-row"><b class="mono">${time}</b> · ${text}</div>`;
  }

  function renderInsights() {
    const signal = state.knowledgeSignal;
    return `
      <section class="page console-page">
        <div class="console-shell card">
          ${renderSidebar("insights")}
          <div class="console-main">
            <header class="console-top"><span class="muted">质量运营 › <b style="color:var(--ink)">知识与产品信号</b></span><span class="health"><i></i>数据链路正常</span></header>
            <div class="console-content">
              <div class="page-title"><div><div class="eyebrow">Learning loop</div><h2>从客服结果形成可审核的改进队列</h2><p>真实结案不会自动训练或发布，而是进入人工可审查的知识、流程与产品信号。</p></div><button class="button primary" data-action="export-summary">导出试点摘要</button></div>
              <div class="kpi-row">${kpi("扫码会话", "1,284", "近 30 天")}${kpi("自主解决率", "71%", "+3.4%")}${kpi("安全拦截", signal ? "18" : "17", "0 次越权继续")}${kpi("交接包完整率", "86%", "目标 ≥ 90%")}</div>
              <div class="insight-grid">
                <section class="signal-list card">
                  <div class="queue-head">待审查信号 <span class="tag ai" style="float:right">人工审核</span></div>
                  <div class="signal-row"><div><h3>FW 2.1.0 网络故障集中</h3><p>X100 / EU · Wi-Fi 连接失败近 7 天上升 28%，建议检查固件变更。</p><span class="tag human">产品质量</span></div><div class="signal-count"><b>${signal ? "19" : "18"}</b><small>相关会话</small></div></div>
                  <div class="signal-row"><div><h3>烧焦气味命中安全停止</h3><p>所有命中均停止自动诊断；${signal ? "最新工单 HAND-240817-38 已确认硬件故障。" : "等待最新人工结案。"}</p><span class="tag safety">安全策略</span></div><div class="signal-count"><b>${signal ? "6" : "5"}</b><small>相关会话</small></div></div>
                  <div class="signal-row"><div><h3>交接包缺少购买凭证</h3><p>14% 的换机工单需要人工再次索要订单号，建议在转人工前增设低摩擦收集步骤。</p><span class="tag ai">流程优化</span></div><div class="signal-count"><b>12</b><small>重复追问</small></div></div>
                </section>
                <aside>
                  <div class="knowledge-card card"><h3>试点成功门槛</h3><div class="funnel">${funnel("设备识别成功", 96, "96%")}${funnel("有依据回答", 89, "89%")}${funnel("交接包完整", 86, "86%")}${funnel("用户无需复述", 78, "78%")}</div><div class="decision-note"><b>当前判断</b><br />技术闭环已具备，但试点上线前应优先补齐交接包完整率与真实外部客服渠道验证。自主解决率只能在真实客户流量中判断。</div></div>
                  <div class="knowledge-card card" style="margin-top:14px"><h3>下一轮原型要验证</h3><p class="muted">1. 图片识别确认流程<br />2. WhatsApp / Chatwoot 外部转接<br />3. 购买凭证与隐私同意<br />4. 流程编辑、审核、发布回滚</p><button class="button ai wide" data-action="go" data-view="brief">返回验证目标</button></div>
                </aside>
              </div>
            </div>
          </div>
        </div>
      </section>`;
  }

  function funnel(label, width, value) {
    return `<div class="funnel-step"><header><span>${label}</span><b>${value}</b></header><div><i style="width:${width}%"></i></div></div>`;
  }

  function escapeHtml(value) {
    return String(value || "").replace(/[&<>"]/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[char]);
  }

  document.addEventListener("click", (event) => {
    const target = event.target.closest("[data-action]");
    if (!target) return;
    event.preventDefault();
    const action = target.dataset.action;
    if (action === "go") {
      const patch = { view: target.dataset.view };
      if (target.dataset.stage) patch.supportStage = target.dataset.stage;
      setState(patch);
    }
    if (action === "start-demo") setState({ view: "support", supportStage: "identify", path: [] });
    if (action === "begin-diagnosis") setState({ supportStage: "diagnose", path: [] });
    if (action === "choose-wifi") setState({ path: ["wifi"] });
    if (action === "networks-yes") setState({ path: [...state.path, "networks-yes"] });
    if (action === "restart-no") setState({ path: [...state.path, "restart-no"] });
    if (action === "heat-yes") setState({ path: [...state.path, "heat-yes"], handoffStatus: "new" }, "已创建高风险人工交接包 HAND-240817-38");
    if (action === "open-handoff") setState({ view: "handoff" });
    if (action === "claim") setState({ handoffStatus: "claimed" }, "工单已由 Maya Chen 接管");
    if (action === "close-case") setState({ handoffStatus: "closed", knowledgeSignal: true }, "结案完成，已生成一条待审查产品质量信号");
    if (action === "lang") setState({ language: target.dataset.lang });
    if (action === "toast") showToast(target.dataset.message);
    if (action === "export-summary") showToast("试点摘要已模拟生成：pilot-summary-2026-08-17.pdf");
    if (action === "reset") {
      state = { ...defaultState };
      saveState();
      render();
      showToast("演示状态已重置");
    }
  });

  document.addEventListener("change", (event) => {
    if (event.target.dataset.action === "resolution") setState({ resolution: event.target.value });
  });

  document.addEventListener("input", (event) => {
    if (event.target.dataset.action === "resolution-note") {
      state.resolutionNote = event.target.value;
      saveState();
    }
  });

  render();
})();
