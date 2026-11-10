(() => {
  const targetKey = "trener-ai:section-after-navigation";

  const saveTarget = (target) => {
    if (target) sessionStorage.setItem(targetKey, target);
  };

  document.addEventListener("click", (event) => {
    const link = event.target.closest("a[data-scroll-target]");
    if (link) saveTarget(link.dataset.scrollTarget);
  }, true);

  document.addEventListener("submit", (event) => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement) || form.method.toLowerCase() !== "post") return;

    const action = new URL(form.action, window.location.href);
    if (action.pathname === "/logout") {
      sessionStorage.removeItem(targetKey);
      return;
    }

    const submitterTarget = event.submitter?.dataset.scrollTarget;
    saveTarget(submitterTarget || form.dataset.scrollTarget);
  }, true);

  const hashTarget = decodeURIComponent(window.location.hash.slice(1));
  const savedTarget = sessionStorage.getItem(targetKey);
  sessionStorage.removeItem(targetKey);
  const targetId = hashTarget || savedTarget;
  if (!targetId) return;

  if ("scrollRestoration" in history) history.scrollRestoration = "manual";

  const restoreSection = () => {
    const target = document.getElementById(targetId);
    if (target) target.scrollIntoView({ block: "start" });
  };

  requestAnimationFrame(() => requestAnimationFrame(restoreSection));
  window.addEventListener("load", restoreSection, { once: true });
})();
