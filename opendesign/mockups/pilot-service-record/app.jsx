const { useEffect, useState } = React;

const STORAGE = "hardware-support-service-record-v3";
const initial = {
  view: "intake",
  language: "zh",
  steps: [],
  handoff: "new",
  handoffReason: null,
  outcome: null,
  resolution: "hardware_failure",
  note: "",
  signalAdded: false,
};

const tr = (language, zh, en) => language === "zh" ? zh : en;

function App() {
  const [state, setState] = useState(() => {
    try { return { ...initial, ...JSON.parse(localStorage.getItem(STORAGE) || "{}") }; }
    catch { return initial; }
  });
  const [toast, setToast] = useState(null);

  useEffect(() => { localStorage.setItem(STORAGE, JSON.stringify(state)); }, [state]);
  useEffect(() => {
    document.documentElement.lang = state.language === "zh" ? "zh-CN" : "en";
    document.title = tr(state.language, "Hardware AI · 设备服务闭环原型", "Hardware AI · Device Support Loop Prototype");
  }, [state.language]);
  useEffect(() => { window.scrollTo({ top: 0, left: 0, behavior: "auto" }); }, [state.view]);
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 3600);
    return () => clearTimeout(timer);
  }, [toast]);

  const update = (patch, message) => {
    setState((current) => ({ ...current, ...patch }));
    if (message) setToast(message);
  };
  const setLanguage = (language) => { setState((current) => ({ ...current, language })); setToast(null); };
  const reset = () => {
    setState({ ...initial, language: state.language });
    setToast({ tone: "info", title: tr(state.language, "演示已重新开始", "Demo restarted"), detail: tr(state.language, "设备与诊断记录已恢复到初始状态。", "The device and diagnostic record have been restored to their initial state.") });
  };

  return <div className="shell">
    <IndexRail view={state.view} language={state.language} go={(view) => update({ view })} />
    <main className="workspace">
      <Utility view={state.view} language={state.language} setLanguage={setLanguage} reset={reset} />
      {state.view === "intake" && <Intake state={state} update={update} />}
      {state.view === "diagnosis" && <Diagnosis state={state} update={update} />}
      {state.view === "handoff" && <Handoff state={state} update={update} />}
      {state.view === "review" && <Review state={state} update={update} />}
      {state.view === "poc" && <PocPlan language={state.language} update={update} />}
    </main>
    {toast && <div className={`toast ${toast.tone || "info"}`} role={toast.tone === "safety" ? "alert" : "status"}><span className="toast-mark" /><div><strong>{toast.title}</strong>{toast.detail && <p>{toast.detail}</p>}</div></div>}
  </div>;
}

function IndexRail({ view, language, go }) {
  const tt = (zh, en) => tr(language, zh, en);
  const items = [["intake", "01", tt("设备档案", "Device")], ["diagnosis", "02", tt("诊断记录", "Diagnosis")], ["handoff", "03", tt("人工接管", "Handoff")], ["review", "04", tt("改进队列", "Review")], ["poc", "05", tt("海外 POC", "Global POC")]];
  return <aside className="index-rail">
    <div className="wordmark"><b>HARDWARE AI</b><small>{tt("设备服务台", "Device support")}</small></div>
    <nav className="section-nav" aria-label={tt("原型章节", "Prototype sections")}>{items.map(([id, no, name]) =>
      <button key={id} className={view === id ? "active" : ""} onClick={() => go(id)} aria-label={`${no} ${name}`}>
        <span className="nav-no">{no}</span><span className="nav-name">{name}</span>
      </button>)}</nav>
    <div className="rail-meta">{tt("试点", "PILOT")} / X100-EU<br />{tt("版本", "REV")} <b>2026.08</b><br />{tt("本地交互原型", "LOCAL PROTOTYPE")}</div>
  </aside>;
}

function Utility({ view, language, setLanguage, reset }) {
  const tt = (zh, en) => tr(language, zh, en);
  const names = { intake: tt("设备档案", "Device profile"), diagnosis: tt("诊断记录", "Diagnostic record"), handoff: tt("人工接管", "Human handoff"), review: tt("改进队列", "Improvement queue"), poc: tt("海外 POC 与落地", "Global POC and rollout") };
  return <header className="utility"><span className="crumb">{tt("试点闭环", "Pilot loop")} / {names[view]}</span><span className="session mono">{tt("会话", "SESSION")} A1F3 · 09:41 CET</span><div className="global-language" role="group" aria-label={tt("界面语言", "Interface language")}><button className={language === "zh" ? "active" : ""} onClick={() => setLanguage("zh")}>中文</button><button className={language === "en" ? "active" : ""} onClick={() => setLanguage("en")}>English</button></div><button onClick={reset}>{tt("重置", "Reset")}</button></header>;
}

function PageHead({ no, title, language, children }) {
  return <><div className="page-kicker">{tr(language, `第 ${no} 章 / 试点服务闭环`, `Section ${no} / Pilot service loop`)}</div><div className="title-row"><h1>{title}</h1><div className="title-note">{children}</div></div></>;
}

function Intake({ state, update }) {
  const tt = (zh, en) => tr(state.language, zh, en);
  const begin = () => update({ view: "diagnosis", steps: [], outcome: null, handoffReason: null, handoff: "new", note: "" });
  return <section className="page">
    <PageHead no="01" language={state.language} title={tt(<>先确认设备，<br />再开始回答。</>, <>Confirm the device<br />before answering.</>)}><b>{tt("为什么先做这一步", "Why this comes first")}</b>{tt("型号、硬件版本、固件和销售地区共同决定哪些说明与流程可以使用。", "The model, hardware revision, firmware and sales region determine which guidance and flows apply.")}</PageHead>
    <div className="intake-grid">
      <article className="device-sheet">
        <div className="sheet-top"><h2>{tt("设备服务档案", "Device service profile")}</h2><div className="sheet-id">{tt("档案编号", "RECORD ID")}<br /><b>X100-EU-7F3A91</b></div></div>
        <dl className="fact-table">
          <Fact label={tt("产品", "Product")} value="X100 Android TV Box" />
          <Fact label={tt("硬件版本", "Hardware revision")} value="rev.B" mono />
          <Fact label={tt("当前固件", "Current firmware")} value="2.1.0" mono />
          <Fact label={tt("销售地区", "Sales region")} value="EU" mono />
          <Fact label={tt("保修状态", "Warranty")} value={tt("有效 · 还剩 214 天", "Active · 214 days remaining")} />
          <Fact label={tt("知识范围", "Knowledge scope")} value={tt("6 份已发布文档 / 4 条诊断流程", "6 published documents / 4 diagnostic flows")} />
        </dl>
        <div className="sheet-foot"><span className="status-stamp ok">{tt("二维码已验证", "QR verified")}</span><span className="mono label">09:38:12</span><button className="action primary" onClick={begin}>{tt("确认并开始诊断", "Confirm and start")}</button></div>
      </article>
      <div className="intake-side">
        <span className="label">{tt("用户反馈", "Reported issue")}</span><h2>{tt("用户遇到了什么问题？", "What is the customer experiencing?")}</h2><p>{tt("选一个最接近的现象。系统会使用适用于这台设备的已发布流程，不会把其它型号的步骤混进来。", "Choose the closest symptom. The system will use a published flow for this device and will not mix in steps from other models.")}</p>
        <div className="issue-list">
          <button onClick={begin}><span className="issue-no">01</span><span><strong>{tt("Wi-Fi 无法连接", "Wi-Fi will not connect")}</strong><small>{tt("能看到网络，但连接失败或无响应", "The network is visible, but connection fails or does not respond")}</small></span><span className="issue-arrow">→</span></button>
          <button onClick={() => setToastSafe(update, tt("本轮先演示 Wi-Fi", "This pilot focuses on Wi-Fi"), tt("遥控器诊断将在下一批试点开放，这次不会进入一条不完整的流程。", "Remote-control diagnosis will open in the next pilot batch, so this demo will not enter an incomplete flow."))}><span className="issue-no">02</span><span><strong>{tt("遥控器没有反应", "Remote control not responding")}</strong><small>{tt("按键无响应、配对失败", "Buttons do not respond or pairing fails")}</small></span><span className="issue-arrow">→</span></button>
          <button onClick={() => setToastSafe(update, tt("本轮先演示 Wi-Fi", "This pilot focuses on Wi-Fi"), tt("HDMI 诊断将在下一批试点开放，这次不会进入一条不完整的流程。", "HDMI diagnosis will open in the next pilot batch, so this demo will not enter an incomplete flow."))}><span className="issue-no">03</span><span><strong>{tt("电视没有画面", "No picture on the TV")}</strong><small>{tt("无信号、黑屏、闪烁", "No signal, black screen or flickering")}</small></span><span className="issue-arrow">→</span></button>
        </div>
      </div>
    </div>
  </section>;
}

function setToastSafe(update, title, detail, tone = "info") { update({}, { tone, title, detail }); }
function Fact({ label, value, mono }) { return <div className="fact-row"><dt>{label}</dt><dd className={mono ? "mono" : ""}>{value}</dd></div>; }

function Diagnosis({ state, update }) {
  const has = (step) => state.steps.includes(step);
  const reply = (step, patch = {}) => update({ steps: [...new Set([...state.steps, step])], ...patch });
  const safety = has("heat");
  const safe = has("safe");
  const hidden = has("hidden");
  const bandFailed = has("band-failed");
  const resolved = Boolean(state.outcome);
  const handedOff = Boolean(state.handoffReason);
  const lang = (zh, en) => state.language === "zh" ? zh : en;
  const resolve = (outcome) => update({ outcome }, { tone: "success", title: lang("连接已经恢复", "Connection restored"), detail: lang("这次处理步骤和结果已经整理进服务记录。", "The completed steps and outcome are now saved in the service record.") });
  const escalate = (reason) => update(
    { handoffReason: reason, handoff: "new", resolution: "flow_gap" },
    { tone: "human", title: lang("需要进一步检查", "Further review is needed"), detail: lang("设备、尝试过的步骤和用户回答已经整理好，人工客服可以从这里继续。", "The device, completed steps and customer responses are ready for a human agent to continue.") }
  );
  const restartFlow = () => update({ steps: [], outcome: null, handoffReason: null, handoff: "new" });
  return <section className="page">
    <PageHead no="02" language={state.language} title={lang(<>一次只排查<br />一个判断。</>, <>One decision<br />at a time.</>)}><b>{lang("已发布流程 · Wi-Fi 连接 / v3", "Published flow · Wi-Fi connection / v3")}</b>{lang("当前步骤由状态机决定。安全检查不能跳过，模型不能临时增加拆机或通电测试。", "A state machine determines the current step. Safety checks cannot be skipped, and the model cannot invent disassembly or power-on tests.")}</PageHead>
    <div className="diagnosis-layout">
      <article className="phone">
        <header className="phone-head"><span className="device-letter">X</span><div><strong>{lang("X100 设备支持", "X100 Device Support")}</strong><small>EU · rev.B · FW 2.1.0</small></div><span className="phone-locale">{state.language === "zh" ? "中文" : "English"}</span></header>
        <div className="phone-body">
          <Record time="09:38" kind="system" locale={state.language} title={lang("设备已确认", "Device confirmed")}>{lang("后续步骤仅使用适用于 X100 rev.B、固件 2.1.0 和 EU 地区的内容。", "The next steps only use content for X100 rev.B, firmware 2.1.0 and the EU region.")}</Record>
          <Record time="09:39" kind="user" locale={state.language} title={lang("用户", "Customer")}>{lang("能看到 Wi-Fi，但点连接一直没有反应。", "I can see Wi-Fi, but tapping connect does nothing.")}</Record>
          {!has("visible") && !hidden && <Record time="09:39" locale={state.language} title={lang("检查网络可见性", "Check network visibility")}><Prompt label={lang("判断节点 · wifi-check-01", "Decision · wifi-check-01")} text={lang("设置页里能看到其它 Wi-Fi 名称吗？", "Can you see other Wi-Fi names in Settings?")}><button className="action primary" onClick={() => reply("visible")}>{lang("可以看到", "Yes")}</button><button className="action" onClick={() => reply("hidden")}>{lang("看不到", "No")}</button></Prompt><Citation locale={state.language} /></Record>}
          {has("visible") && <Record time="09:39" kind="user" locale={state.language} title={lang("用户", "Customer")}>{lang("可以看到其它网络。", "I can see other networks.")}</Record>}
          {hidden && <Record time="09:39" kind="user" locale={state.language} title={lang("用户", "Customer")}>{lang("列表里看不到家里的网络。", "My home network does not appear in the list.")}</Record>}
          {hidden && !bandFailed && !resolved && !handedOff && <Record time="09:40" locale={state.language} title={lang("确认频段与距离", "Check band and distance")}><Prompt label={lang("分支节点 · wifi-band-01", "Branch · wifi-band-01")} text={lang("请靠近路由器，并确认 2.4 GHz 网络已经开启。现在能看到并连接吗？", "Move closer to the router and make sure the 2.4 GHz network is enabled. Can you now see and connect to it?")}><button className="action primary" onClick={() => resolve("resolved_band")}>{lang("可以连接了", "Connected")}</button><button className="action" onClick={() => reply("band-failed")}>{lang("还是看不到", "Still not visible")}</button></Prompt><Citation locale={state.language} /></Record>}
          {bandFailed && !handedOff && <Record time="09:41" kind="user" locale={state.language} title={lang("用户", "Customer")}>{lang("靠近路由器并确认频段后，还是看不到。", "It is still not visible after checking distance and band.")}</Record>}
          {bandFailed && !handedOff && <EscalationPanel lang={lang} onContinue={() => escalate("network_unresolved")} />}
          {has("visible") && !has("restart") && !resolved && <Record time="09:40" locale={state.language} title={lang("重启并复测", "Restart and test again")}><Prompt label={lang("操作节点 · wifi-check-02", "Operation · wifi-check-02")} text={lang("断开机顶盒和路由器电源，等待 30 秒，再依次开启。", "Disconnect power from the TV box and router, wait 30 seconds, then restart them in order.")}><button className="action" onClick={() => resolve("resolved_restart")}>{lang("恢复正常", "Resolved")}</button><button className="action primary" onClick={() => reply("restart")}>{lang("仍未解决", "Still failing")}</button></Prompt><Citation locale={state.language} /></Record>}
          {has("restart") && <Record time="09:40" kind="user" locale={state.language} title={lang("用户", "Customer")}>{lang("重启后还是连不上。", "It still does not connect after restarting.")}</Record>}
          {has("restart") && !safety && !safe && <Record time="09:41" locale={state.language} title={lang("强制安全确认", "Required safety check")}><Prompt label={lang("高风险节点 · thermal-check", "High-risk check · thermal-check")} text={lang("设备或电源接口是否发热、有烧焦气味或可见损坏？", "Is the device or power port hot, burnt-smelling, or visibly damaged?")}><button className="action safety" onClick={() => update({ steps: [...state.steps, "heat"], handoff: "new", handoffReason: "safety_burnt", resolution: "hardware_failure" }, { tone: "safety", title: lang("自动诊断已停止", "Automated diagnosis stopped"), detail: lang("请断开电源并保持设备原状，安全情况和诊断记录已经准备好交给人工客服。", "Unplug the device and leave it as-is. The safety details and diagnostic record are ready for a human agent.") })}>{lang("是，有烧焦气味", "Yes, burnt smell")}</button><button className="action" onClick={() => reply("safe")}>{lang("没有异常", "No physical issue")}</button></Prompt></Record>}
          {safe && !resolved && !handedOff && <Record time="09:42" locale={state.language} title={lang("重置网络设置", "Reset network settings")}><Prompt label={lang("操作节点 · wifi-reset-01", "Operation · wifi-reset-01")} text={lang("没有发现物理风险。请打开“设置 › 网络 › 重置网络”，重新选择 Wi-Fi 并输入密码。", "No physical risk was found. Open Settings › Network › Reset network, then select Wi-Fi and enter the password again.")}><button className="action primary" onClick={() => resolve("resolved_reset")}>{lang("已经恢复", "Resolved")}</button><button className="action" onClick={() => escalate("network_unresolved")}>{lang("重置后仍失败", "Still failing")}</button></Prompt><Citation locale={state.language} /></Record>}
          {safety && <div className="safety-stop"><h3>{lang("停止通电", "Stop powering the device")}</h3><p>{lang("请断开电源并保持设备原状。自动诊断已结束；人工客服会收到设备、步骤和风险记录。", "Disconnect power and leave the device as-is. Automated diagnosis has ended; a human agent will receive the device, steps and risk record.")}</p><button className="action human" onClick={() => update({ view: "handoff" })}>{lang("进入人工接管", "Continue to human support")}</button></div>}
          {resolved && <ResolutionPanel lang={lang} outcome={state.outcome} restart={restartFlow} done={() => update({ view: "intake" })} />}
          {handedOff && !safety && <div className="handoff-ready"><span className="label">{lang("人工交接已准备 · P1", "Handoff ready · P1")}</span><h3>{lang("后续检查已经接上", "Further support is ready")}</h3><p>{lang("不需要让用户重新描述。人工客服会看到设备信息、分支选择和已经尝试过的操作。", "The customer will not need to repeat the story. A human agent will see the device, branch choices and completed actions.")}</p><button className="action human" onClick={() => update({ view: "handoff" })}>{lang("查看人工交接", "Review human handoff")}</button></div>}
        </div>
        <footer className="composer"><div className="composer-row"><input disabled placeholder={lang("描述问题或输入错误码…", "Describe the issue or enter an error code…")} /><button className="action small" disabled>{lang("发送", "Send")}</button></div><small>{lang("图片只用于识别错误码、接口和设备标签。", "Images are only used to identify error codes, ports and device labels.")}</small></footer>
      </article>
      <aside className="case-notes">
        <span className="label">{lang("处理进度", "Case progress")}</span><h2>{lang("诊断记录", "Diagnostic record")}</h2>
        <div className="checklist">
          <Check status="done" title={lang("设备与知识范围", "Device and knowledge scope")} code="context-resolved" />
          <Check status={has("visible") || hidden ? "done" : "current"} title={lang("网络可见性", "Network visibility")} code="wifi-check-01" />
          <Check status={hidden ? (bandFailed || resolved ? "done" : "current") : has("restart") || state.outcome === "resolved_restart" ? "done" : has("visible") ? "current" : "pending"} title={hidden ? lang("频段与距离", "Band and distance") : lang("重启与复测", "Restart and retest")} code={hidden ? "wifi-band-01" : "wifi-check-02"} />
          <Check status={safety ? "risk" : safe ? "done" : has("restart") ? "current" : "pending"} title={lang("物理安全检查", "Physical safety check")} code="thermal-check" />
          <Check status={state.outcome === "resolved_reset" || handedOff ? "done" : safe ? "current" : "pending"} title={lang("网络重置 / 后续处理", "Network reset / next action")} code="wifi-reset-01" />
          <Check status={handedOff ? "current" : "pending"} title={lang("人工交接", "Human handoff")} code="HAND-240817-38" />
        </div>
        <div className="margin-note"><span className="label">{lang("设计说明", "Design note")}</span><p>{safety ? lang("安全节点命中后，普通输入与后续操作全部关闭，只保留断电指引和人工联系方式。", "Once a safety rule is triggered, normal input and actions close; only power-off guidance and human contact remain.") : resolved ? lang("正向流程保留完成原因，结束时给用户一个明确、克制的确认。", "A successful path records why it worked and ends with a clear, restrained confirmation.") : lang("不顺利的流程继续给出下一步；只有需要人工判断时才生成交接包。", "An unsuccessful path continues with a next step; a handoff package is created only when human judgment is needed.")}</p></div>
      </aside>
    </div>
  </section>;
}

function ResolutionPanel({ lang, outcome, restart, done }) {
  const detail = outcome === "resolved_restart" ? lang("重启设备与路由器后恢复连接。", "Connection recovered after restarting the device and router.") : outcome === "resolved_band" ? lang("靠近路由器并启用 2.4 GHz 后恢复连接。", "Connection recovered after moving closer and enabling 2.4 GHz.") : lang("重置网络设置并重新输入密码后恢复连接。", "Connection recovered after resetting network settings and entering the password again.");
  return <div className="resolution-panel"><span className="label">{lang("处理结果 · 已解决", "Case outcome · Resolved")}</span><h3>{lang("连接已经恢复", "Connection restored")}</h3><p>{detail}</p><div className="resolution-meta"><span>09:43</span><span>KB-NET-102 v3</span><span>{lang("无需人工接管", "No handoff needed")}</span></div><div className="choice-row"><button className="action primary" onClick={done}>{lang("结束本次支持", "Finish support")}</button><button className="action" onClick={restart}>{lang("演示其他路径", "Try another path")}</button></div></div>;
}

function EscalationPanel({ lang, onContinue }) {
  return <div className="escalation-panel"><span className="label">{lang("下一步 · 人工复核", "Next step · Human review")}</span><h3>{lang("这台设备需要进一步检查", "This device needs a closer look")}</h3><p>{lang("常规排查没有恢复网络，但没有发现需要立即断电的风险。可以把当前记录直接交给人工客服继续。", "Standard checks did not restore the network, but no immediate power-off risk was found. The current record can go directly to a human agent.")}</p><button className="action human" onClick={onContinue}>{lang("准备人工交接", "Prepare human handoff")}</button></div>;
}

function Record({ time, title, kind = "system", locale = "zh", children }) { return <div className={`record-entry ${kind}`}><div className="record-time">{time}<br />{locale === "en" ? (kind === "user" ? "Customer" : "System") : (kind === "user" ? "用户" : "系统")}</div><div className="record-content"><strong>{title}</strong><div>{children}</div></div></div>; }
function Prompt({ label, text, children }) { return <div className="prompt-block"><span className="label">{label}</span><p>{text}</p><div className="choice-row">{children}</div></div>; }
function Citation({ locale = "zh" }) { return <div className="citation-line">{locale === "en" ? "Source" : "依据"} · MANUAL-X100-EU §3.2 / KB-NET-102 v3</div>; }
function Check({ status, title, code }) { return <div className={`check-row ${status}`}><strong>{title}</strong><small>{code}</small></div>; }

function Handoff({ state, update }) {
  const tt = (zh, en) => tr(state.language, zh, en);
  const claimed = state.handoff === "claimed";
  const closed = state.handoff === "closed";
  const safetyCase = state.handoffReason !== "network_unresolved";
  const close = () => {
    if (!state.note.trim()) return update({}, { tone: "human", title: tt("还差一项处理说明", "One action note is still required"), detail: tt("请写明已经确认的处理，以及对用户作出的承诺。", "Describe the confirmed action and the commitment made to the customer.") });
    update({ handoff: "closed", signalAdded: true }, { tone: "success", title: tt("人工处理已经完成", "Human support completed"), detail: tt("结案结果已进入审核队列，不会未经确认直接发布。", "The outcome is in the review queue and will not be published without approval.") });
  };
  return <section className="page">
    <PageHead no="03" language={state.language} title={tt(<>系统交记录，<br />人工做承诺。</>, <>The system passes context.<br />A human makes the commitment.</>)}><b>{tt("内部队列", "Internal queue")} · {safetyCase ? tt("P0 安全工单", "P0 safety") : tt("P1 服务工单", "P1 service")}</b>{tt("人工接手时应直接看到设备、已完成步骤、用户原话和交接原因，不再让用户从头复述。", "When an agent takes over, they should see the device, completed steps, customer wording and handoff reason without asking the customer to start over.")}</PageHead>
    <div className="desk">
      <section className="queue"><div className="desk-head">{tt("优先队列", "Priority queue")} <span className={`status-stamp ${safetyCase ? "safety" : "human"}`}>{safetyCase ? "P0 · 1" : "P1 · 3"}</span></div><button className="ticket active"><strong>{safetyCase ? tt("X100 · 电源接口异味", "X100 · Burnt smell at power port") : tt("X100 · 网络仍不可见", "X100 · Network still not visible")}</strong><p>{tt("Wi-Fi 故障", "Wi-Fi issue")} / EU</p><small>HAND-240817-38 · {tt("2 分钟", "2 MIN")}</small></button><button className="ticket" onClick={() => setToastSafe(update, tt("暂留在当前工单", "Staying on the current case"), tt("第二张工单只用于呈现队列层级，本轮演示聚焦 HAND-240817-38。", "The second ticket only demonstrates queue hierarchy; this demo stays focused on HAND-240817-38."))}><strong>{tt("X100 · 遥控器失灵", "X100 · Remote not responding")}</strong><p>{tt("按键无响应", "Buttons not responding")} / UK</p><small>HAND-240817-37 · {tt("8 分钟", "8 MIN")}</small></button></section>
      <section className="case"><div className="desk-head">HAND-240817-38 <span className={`status-stamp ${closed ? "ok" : claimed ? "human" : safetyCase ? "safety" : "ai"}`}>{closed ? tt("已结案", "Closed") : claimed ? tt("处理中", "In progress") : tt("新工单", "New")}</span></div><div className="case-summary"><h3>{tt("系统整理 · 未经人工确认", "System summary · Not yet confirmed by an agent")}</h3><p>{safetyCase ? tt("用户反馈 Wi-Fi 无法连接；完成网络可见性、重启与复测后仍未解决。用户确认电源接口有烧焦气味，系统已停止继续通电。", "The customer reports that Wi-Fi will not connect. Network visibility and restart checks did not resolve it. The customer confirmed a burnt smell at the power port, so the system stopped further power-on steps.") : tt("用户反馈 Wi-Fi 无法连接；常规排查没有恢复网络。设备未发现发热、异味或可见损坏，当前记录已完整交接，等待人工进一步判断。", "The customer reports that Wi-Fi will not connect. Standard checks did not restore the network. No heat, smell or visible damage was found; the complete record is ready for further human review.")}</p></div><div className="timeline"><Time t="09:38" text={tt("识别 X100 / rev.B / FW 2.1.0 / EU", "Identified X100 / rev.B / FW 2.1.0 / EU")} /><Time t="09:39" text={safetyCase ? tt("网络可见性：能发现其它 SSID", "Network visibility: other SSIDs found") : tt("网络可见性与基础连接检查完成", "Network visibility and basic connection checks completed")} /><Time t="09:40" text={safetyCase ? tt("重启与复测：未解决", "Restart and retest: unresolved") : tt("频段、距离或网络重置：仍未解决", "Band, distance or network reset: unresolved")} /><Time t="09:41" text={safetyCase ? tt("高风险：烧焦气味；停止自动诊断", "High risk: burnt smell; automated diagnosis stopped") : tt("未发现物理风险；转人工进一步检查", "No physical risk found; routed for human review")} />{closed && <Time t="09:45" text={safetyCase ? tt("人工确认硬件故障，安排保修换机", "Agent confirmed hardware failure and arranged warranty replacement") : tt("人工确认后续方案，并向用户说明处理时限", "Agent confirmed the next action and response time") } />}</div><div className="case-actions">{!claimed && !closed && <button className="action human" onClick={() => update({ handoff: "claimed" }, { tone: "human", title: tt("Maya Chen 正在接手", "Maya Chen is taking over"), detail: tt("设备、步骤和用户回答已经完整交接，无需再次询问。", "The device, steps and customer responses are already available; no repetition is needed.") })}>{tt("由我接管", "Take over")}</button>}{claimed && <button className="action primary" onClick={close}>{tt("确认结案并回流", "Close and send to review")}</button>}{closed && <button className="action" onClick={() => update({ view: "review" })}>{tt("查看改进队列", "View improvement queue")}</button>}</div></section>
      <aside className="dossier"><div className="desk-head" style={{padding: 0}}>{tt("交接档案", "Handoff dossier")}</div><div className="dossier-block"><span className="label">{tt("设备信息", "Device")}</span><div className="mono">X100 · rev.B<br />FW 2.1.0 · EU<br />QR 7F3…A91</div></div><div className="dossier-block"><span className="label">{tt("处理依据", "Evidence")}</span><span className="status-stamp ai">KB-NET-102 v3</span> {safetyCase ? <span className="status-stamp safety">{tt("命中安全规则", "Safety hit")}</span> : <span className="status-stamp human">{tt("等待人工复核", "Human review")}</span>}</div><div className="dossier-block"><label className="label" htmlFor="resolution">{tt("结案原因", "Resolution reason")}</label><select id="resolution" value={state.resolution} onChange={(e) => update({ resolution: e.target.value })}><option value="hardware_failure">{tt("硬件故障 / 换机", "Hardware failure / Replacement")}</option><option value="knowledge_gap">{tt("知识缺口", "Knowledge gap")}</option><option value="flow_gap">{tt("流程缺口", "Flow gap")}</option></select></div><div className="dossier-block"><label className="label" htmlFor="note">{tt("人工处理备注 · 必填", "Agent action note · Required")}</label><textarea id="note" value={state.note} onChange={(e) => update({ note: e.target.value })} placeholder={safetyCase ? tt("例如：已确认断电，创建换机单，24 小时内邮件联系…", "Example: Power-off confirmed; replacement created; email within 24 hours…") : tt("例如：已收集路由器型号，升级二线检查，4 小时内回电…", "Example: Router model collected; escalated to tier 2; call back within 4 hours…")} /></div></aside>
    </div>
  </section>;
}

function Time({ t, text }) { return <div className="time-row"><time>{t}</time><span>{text}</span></div>; }

function Review({ state, update }) {
  const tt = (zh, en) => tr(state.language, zh, en);
  return <section className="page">
    <PageHead no="04" language={state.language} title={tt(<>结案不是终点，<br />也不自动学习。</>, <>Closing a case is not the end.<br />Nothing learns automatically.</>)}><b>{tt("人工审核队列", "Human review queue")}</b>{tt("客服结果先被分类为知识、流程或产品信号。只有审核通过的内容才会进入正式知识和诊断流程。", "Support outcomes are first classified as knowledge, flow or product signals. Only approved items enter published knowledge and diagnostic flows.")}</PageHead>
    <div className="review-grid">
      <section><span className="label">{tt("待审核信号", "Signals awaiting review")}</span><div className="signal-table"><Signal title={tt("FW 2.1.0 网络故障集中", "FW 2.1.0 network failures clustering")} text={tt("X100 / EU · 近 7 天 Wi-Fi 连接失败上升 28%，建议核查固件变更。", "X100 / EU · Wi-Fi connection failures rose 28% over 7 days; review the firmware change.")} type={tt("产品质量", "Product quality")} related={tt("关联工单", "RELATED CASES")} count={state.signalAdded ? 19 : 18} tone="human" /><Signal title={tt("烧焦气味触发安全停止", "Burnt smell triggers safety stop")} text={state.signalAdded ? tt("最新工单 HAND-240817-38 已由人工确认硬件故障。", "The latest case, HAND-240817-38, has been confirmed as hardware failure by an agent.") : tt("全部命中均停止自动诊断，最新工单等待人工结案。", "Every match stopped automated diagnosis; the latest case awaits human closure.")} type={tt("安全策略", "Safety policy")} related={tt("关联工单", "RELATED CASES")} count={state.signalAdded ? 6 : 5} tone="safety" /><Signal title={tt("换机交接缺少购买凭证", "Replacement handoffs lack proof of purchase")} text={tt("14% 的换机工单需要再次索要订单号，建议增加低摩擦收集步骤。", "14% of replacement cases require another request for the order number; add a low-friction collection step.")} type={tt("流程优化", "Flow improvement")} related={tt("关联工单", "RELATED CASES")} count={12} tone="ai" /></div></section>
      <aside className="decision-sheet"><span className="label">{tt("试点决策表", "Pilot decision sheet")}</span><h2>{tt("试点门槛", "Pilot thresholds")}</h2><Metric name={tt("设备识别成功", "Device identified")} value="96%" /><Metric name={tt("有依据回答", "Evidence-backed answers")} value="89%" /><Metric name={tt("交接包完整", "Complete handoff package")} value="86%" /><Metric name={tt("用户无需复述", "No customer repetition")} value="78%" /><div className="decision"><b>{tt("当前判断", "Current assessment")}</b><p>{tt("闭环可以进入真实试点，但外部人工渠道和交接包完整率仍是上线前阻塞项。", "The loop can enter a live pilot, but a real human channel and handoff completeness remain launch blockers.")}</p></div><span className="label">{tt("下次评审", "Next review")}</span><ol className="next-list"><li>{tt("图片识别确认流程", "Image recognition confirmation")}</li><li>{tt("真实外部客服渠道", "Real external support channel")}</li><li>{tt("购买凭证与隐私同意", "Proof of purchase and privacy consent")}</li><li>{tt("流程审核与回滚", "Flow review and rollback")}</li></ol><button className="action primary" style={{width:"100%", marginTop:18}} onClick={() => update({ view: "poc" })}>{tt("查看海外 POC 方案", "View global POC plan")}</button></aside>
    </div>
  </section>;
}

function PocPlan({ language, update }) {
  const tt = (zh, en) => tr(language, zh, en);
  const references = language === "zh" ? [
    ["Chatwoot", "GitHub 35k+", "共享收件箱、会话状态、分配和联系人上下文", "人工工作台采用队列—会话—设备档案三栏结构", "https://github.com/chatwoot/chatwoot"],
    ["Typebot", "GitHub 10k+", "气泡、输入、逻辑和集成分开编排", "用户端一次只提出一个判断，按钮直接进入下一分支", "https://github.com/baptisteArno/typebot.io"],
    ["Dify", "GitHub 152k+", "工作流、知识检索、模型管理与运行观测", "管理能力与用户对话分离，模型不负责硬件安全跳转", "https://github.com/langgenius/dify"],
    ["Flowise", "GitHub 55k+", "显式分支、人工介入、嵌入式聊天组件", "异常路径保留检查点，人工接手后可以继续而非重开会话", "https://github.com/FlowiseAI/Flowise"],
    ["Langflow", "GitHub 153k+", "组件化流程、测试工作台、API 与 MCP 接入", "企业适配与模型适配放在核心闭环外侧，可单独替换和测试", "https://github.com/langflow-ai/langflow"],
  ] : [
    ["Chatwoot", "GitHub 35k+", "Shared inbox, conversation status, assignment and contact context", "Use a three-column queue, conversation and device-profile agent workspace", "https://github.com/chatwoot/chatwoot"],
    ["Typebot", "GitHub 10k+", "Separate bubbles, inputs, logic and integrations", "Ask one decision at a time and let each action enter the next branch directly", "https://github.com/baptisteArno/typebot.io"],
    ["Dify", "GitHub 152k+", "Workflow, knowledge retrieval, model management and runtime observability", "Separate administration from conversations; models do not control hardware safety branches", "https://github.com/langgenius/dify"],
    ["Flowise", "GitHub 55k+", "Explicit branches, human intervention and embedded chat", "Keep checkpoints on exception paths so a human can continue instead of restarting", "https://github.com/FlowiseAI/Flowise"],
    ["Langflow", "GitHub 153k+", "Component workflows, test playground, API and MCP integration", "Keep company and model adapters outside the core loop so they can be replaced and tested", "https://github.com/langflow-ai/langflow"],
  ];
  const challenges = language === "zh" ? [
    ["01", "知识冷启动", "说明书、FAQ 和历史工单口径冲突，直接向量化会放大错误。", "先做权威源分级、版本审核和 50–100 条金标问答；无证据时拒答或转人工。"],
    ["02", "设备版本错配", "同一产品在硬件版本、固件与销售地区上步骤不同。", "扫码写入租户、型号、硬件版本、固件和地区；检索与流程先做适用性过滤。"],
    ["03", "英语本地化", "翻译正确不等于当地用户读得懂，术语、保修和安全表达也因市场不同。", "建立 en-US 与 en-GB 术语和语气基线，并由当地客服审核 100 条真实对话。"],
    ["04", "跨时区人工接管", "转人工后可能无人在线，或者用户被迫重新描述问题。", "接入客户现有收件箱、邮件或 WhatsApp；交接包携带设备、步骤、原话和服务时限。"],
    ["05", "私有化与隐私", "客户要求数据留在本地，但外部模型仍可能收到文本或图片。", "单客户独立实例；个人信息脱敏和最小上下文；模型是唯一受控出口，并提供 VPC 与离线档位。"],
    ["06", "企业系统接入", "每家公司使用的 CRM、工单、知识库、身份和字段都不同。", "核心业务域保持稳定，外侧使用渠道、知识、工单和身份适配器；先接通一个真实渠道再扩展。"],
    ["07", "多模型与升级", "模型价格、能力、区域可用性持续变化，绑定单一模型会锁死交付。", "统一 OpenAI 兼容模型接口，按任务路由；上线前固定版本，并用同一评测集回归。"],
    ["08", "反馈污染", "人工结案和用户点赞不天然等于正确答案。", "反馈只进入审核队列；知识、流程、提示词和模型变更都要经过离线回归与小流量发布。"],
  ] : [
    ["01", "Knowledge cold start", "Manuals, FAQs and historical tickets may conflict; direct indexing can amplify bad guidance.", "Rank source authority, review versions and build 50–100 gold-standard cases; refuse or hand off when evidence is missing."],
    ["02", "Device-version mismatch", "The same product may require different steps by hardware revision, firmware and sales region.", "Bind tenant, model, revision, firmware and region at scan time, then filter knowledge and flows by applicability."],
    ["03", "English localization", "A correct translation may still feel unclear locally; terminology, warranty and safety language vary by market.", "Create en-US and en-GB terminology and tone baselines, then have local agents review 100 real conversations."],
    ["04", "Cross-time-zone handoff", "A human may be offline after escalation, or the customer may be forced to repeat the issue.", "Connect the existing inbox, email or WhatsApp and include device, steps, customer wording and SLA in the handoff."],
    ["05", "Private deployment and privacy", "Customers may require local data residency while an external model could still receive text or images.", "Use a dedicated instance, PII redaction and minimum context; keep one controlled model egress with VPC and offline tiers."],
    ["06", "Enterprise systems integration", "Every company has different CRM, ticketing, knowledge, identity and field conventions.", "Keep the core domain stable and use channel, knowledge, ticket and identity adapters; prove one real channel before expanding."],
    ["07", "Multi-model portability", "Model price, capability and regional availability keep changing; one hard dependency creates lock-in.", "Use an OpenAI-compatible provider contract and task-based routing; pin releases and rerun the same evaluation set."],
    ["08", "Feedback contamination", "A closed ticket or thumbs-up is not automatically correct ground truth.", "Send feedback to review first; gate knowledge, flow, prompt and model changes with regression tests and staged rollout."],
  ];
  return <section className="page poc-page">
    <PageHead no="05" language={language} title={tt(<>先证明闭环，<br />再扩大智能。</>, <>Prove the support loop<br />before expanding intelligence.</>)}><b>{tt("英语市场 POC · 6–8 周", "English-market POC · 6–8 weeks")}</b>{tt("试点不是展示模型有多会聊天，而是证明设备识别、可靠回答、受控诊断、人工交接和持续改进能在真实业务里成立。", "The pilot is not a chatbot showcase. It must prove device identification, reliable answers, controlled diagnosis, human handoff and continuous improvement in a real business.")}</PageHead>

    <section className="reference-board">
      <div className="section-intro"><span className="label">{tt("开源产品参考", "Open-source references")}</span><h2>{tt("这套界面从哪里来", "Where this interface comes from")}</h2><p>{tt("参考活跃开源项目的信息架构和交互方式，提炼适合硬件售后的模式，不复制它们的品牌视觉。GitHub 热度记录于 2026-08-17。", "We studied active open-source products and applied the patterns that fit hardware support without copying their brand visuals. GitHub activity was recorded on 2026-08-17.")}</p></div>
      <div className="reference-table">{references.map(([name, heat, pattern, use, href]) => <a href={href} target="_blank" rel="noreferrer" className="reference-row" key={name}><strong>{name}<small>{heat}</small></strong><span>{pattern}</span><span>{use}</span><b>↗</b></a>)}</div>
    </section>

    <section className="poc-band">
      <div className="section-intro inverse"><span className="label">{tt("试点步骤", "POC sequence")}</span><h2>{tt("一个当地试点，分四段推进", "Run one local pilot in four stages")}</h2></div>
      <ol className="poc-steps"><PocStep no="01" title={tt("业务取样", "Business sampling")} text={tt("选 1 家厂商、1 个英语市场、1–2 个型号；访谈售后并整理 100–300 条真实工单。", "Choose one manufacturer, one English-speaking market and 1–2 models; interview support staff and collect 100–300 real tickets.")} /><PocStep no="02" title={tt("影子评测", "Shadow evaluation")} text={tt("AI 不直接服务用户，先跑历史问题；按型号适用性、引用正确性和安全分支验收。", "Run historical cases without serving users; evaluate model applicability, citation accuracy and safety branches.")} /><PocStep no="03" title={tt("受控流量", "Controlled traffic")} text={tt("二维码入口开放给 5%–10% 用户；仅覆盖 3–5 个高频问题，真实接通人工渠道。", "Open the QR entry to 5%–10% of users, cover only 3–5 frequent issues and connect a real human channel.")} /><PocStep no="04" title={tt("扩量决策", "Scale decision")} text={tt("连续两周达到门槛再扩型号、地区和渠道；未达标则回到知识或流程修订。", "Expand models, regions and channels only after two weeks above threshold; otherwise revise knowledge or flows.")} /></ol>
    </section>

    <section className="integration-map">
      <div className="section-intro"><span className="label">{tt("系统接入架构", "Integration architecture")}</span><h2>{tt("接不同公司，不改核心闭环", "Integrate different companies without changing the core loop")}</h2><p>{tt("差异放进适配层；设备上下文、检索、状态机、交接包和审计保持统一。", "Put company differences in adapters while keeping device context, retrieval, state machine, handoff package and audit consistent.")}</p></div>
      <div className="integration-lanes"><Lane title={tt("客户渠道", "Customer channels")} items={[tt("二维码 / 网页", "QR / Web"), tt("电子邮件", "Email"), "WhatsApp", tt("应用 SDK", "App SDK")]} /><span className="lane-arrow">→</span><Lane title={tt("企业适配层", "Company adapters")} items={[tt("单点登录 / 身份", "SSO / Identity"), tt("CRM / 工单", "CRM / Ticket"), tt("知识同步", "Knowledge sync"), tt("字段映射", "Field mapping")]} tone="human" /><span className="lane-arrow">→</span><Lane title={tt("客服核心", "Support core")} items={[tt("产品上下文", "Product context"), tt("检索与引用", "RAG + citation"), tt("诊断状态机", "Flow state machine"), tt("人工交接包", "Handoff package")]} tone="ai" /><span className="lane-arrow">→</span><Lane title={tt("模型与数据", "Model and data")} items={[tt("模型路由", "Provider router"), tt("OpenAI 兼容接口", "OpenAI-compatible"), tt("本地模型", "Local models"), tt("审计与评测", "Audit and evaluation")]} /></div>
    </section>

    <section className="challenge-section">
      <div className="section-intro"><span className="label">{tt("已知难点", "Known hard parts")}</span><h2>{tt("项目难点与对应解法", "Project challenges and responses")}</h2><p>{tt("这些不是“后续优化项”，而是 POC 是否可信的验收对象。", "These are not later enhancements; they are acceptance criteria for a credible POC.")}</p></div>
      <div className="challenge-table">{challenges.map(([no, title, risk, answer]) => <div className="challenge-row" key={no}><span className="challenge-no">{no}</span><h3>{title}</h3><div className="challenge-copy"><span className="label">{tt("问题", "Risk")}</span><p>{risk}</p></div><div className="challenge-copy challenge-answer"><span className="label">{tt("解决方案", "Response")}</span><p>{answer}</p></div></div>)}</div>
    </section>

    <section className="localization-review">
      <div className="section-intro"><span className="label">{tt("需要对照时才并列", "Side by side only when comparison is required")}</span><h2>{tt("本地化校对样例", "Localization review sample")}</h2><p>{tt("中英对照只用于产品团队审核术语和安全语气；真实用户界面一次只显示所选语言。", "Side-by-side languages are only for product teams reviewing terminology and safety tone; the live interface shows one selected language at a time.")}</p></div>
      <div className="localization-grid"><div><span className="label">{tt("中文原意", "Chinese source")}</span><strong>断开电源，不要再次通电，并保持设备原状。</strong></div><div><span className="label">{tt("美国英语 · en-US", "US English · en-US")}</span><strong>Unplug the device. Do not power it on again, and leave it as it is.</strong></div><div><span className="label">{tt("英国英语 · en-GB", "UK English · en-GB")}</span><strong>Disconnect the device from the mains. Do not switch it on again, and leave it as it is.</strong></div></div>
    </section>

    <section className="poc-conclusion">
      <div><span className="label">{tt("完成标准", "Definition of done")}</span><h2>{tt("POC 通过线", "POC acceptance thresholds")}</h2><div className="gate-grid"><Metric name={tt("型号上下文正确", "Correct model context")} value="≥98%" /><Metric name={tt("有依据回答", "Evidence-backed answers")} value="≥90%" /><Metric name={tt("安全节点漏检", "Missed safety nodes")} value="0" /><Metric name={tt("交接包完整", "Complete handoff package")} value="≥95%" /><Metric name={tt("无需重复描述", "No customer repetition")} value="≥85%" /><Metric name={tt("英语客服认可", "Local agent approval")} value="≥4/5" /></div></div>
      <aside><span className="status-stamp human">{tt("结论", "Conclusion")}</span><h2>{tt("最终判断", "Final assessment")}</h2><p>{tt("首个 POC 应卖“可靠的售后闭环”，不是“万能聊天机器人”。先把一个客户、一个市场、少量型号和真实人工渠道跑通，再用适配器扩企业、用模型接口扩供应商、用评测集控制每次变化。", "The first POC should sell a reliable support loop, not a universal chatbot. Prove one customer, one market, a few models and a real human channel; then scale companies with adapters, providers with a model interface and every change with evaluation sets.")}</p><button className="action primary" onClick={() => update({ view: "intake" })}>{tt("重新体验服务流程", "Restart the service flow")}</button></aside>
    </section>
  </section>;
}

function PocStep({ no, title, text }) { return <li><span>{no}</span><h3>{title}</h3><p>{text}</p></li>; }
function Lane({ title, items, tone = "plain" }) { return <div className={`lane ${tone}`}><strong>{title}</strong>{items.map((item) => <span className="lane-item" key={item}>{item}</span>)}</div>; }

function Signal({ title, text, type, related, count, tone }) { return <div className="signal-item"><div><h3>{title}</h3><p>{text}</p><span className={`status-stamp ${tone}`}>{type}</span></div><div className="signal-number"><b>{count}</b><small>{related}</small></div></div>; }
function Metric({ name, value }) { return <div className="metric-row"><span>{name}</span><b>{value}</b></div>; }

ReactDOM.createRoot(document.getElementById("root")).render(<App />);
