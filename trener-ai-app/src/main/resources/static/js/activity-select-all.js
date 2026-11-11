(() => {
  const STORAGE_KEY = "trener-selected-activities";
  const selectAll = document.getElementById("select-all-activities");
  const activityList = document.querySelector(".activity-list");
  if (!selectAll || !activityList) return;

  const allIdsAttr = activityList.dataset.allIds || "";
  const allIds = new Set(allIdsAttr ? allIdsAttr.split(",") : []);
  const pageCheckboxes = Array.from(
    activityList.querySelectorAll('input[name="activityIds"]')
  );

  function loadSelection() {
    try {
      const json = sessionStorage.getItem(STORAGE_KEY);
      return json ? new Set(JSON.parse(json)) : null;
    } catch (_) {
      return null;
    }
  }

  function saveSelection(ids) {
    try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify([...ids])); } catch (_) {}
  }

  let selected = loadSelection() || new Set(
    pageCheckboxes.filter(cb => cb.checked).map(cb => cb.value)
  );

  function applyToPage() {
    pageCheckboxes.forEach(cb => { cb.checked = selected.has(cb.value); });
    const allSelected = allIds.size > 0 && allIds.size === selected.size
        && [...allIds].every(id => selected.has(id));
    const someSelected = selected.size > 0;
    selectAll.checked = allSelected;
    selectAll.indeterminate = someSelected && !allSelected;
  }

  function injectHiddenInputs() {
    const container = document.getElementById("off-page-selections");
    if (!container) return;
    container.innerHTML = "";
    const pageIds = new Set(pageCheckboxes.map(cb => cb.value));
    selected.forEach(id => {
      if (!pageIds.has(id)) {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = "activityIds";
        input.value = id;
        container.appendChild(input);
      }
    });
  }

  selectAll.addEventListener("change", () => {
    if (selectAll.checked) {
      selected = new Set(allIds);
    } else {
      selected = new Set();
    }
    saveSelection(selected);
    applyToPage();
    injectHiddenInputs();
  });

  pageCheckboxes.forEach(cb => {
    cb.addEventListener("change", () => {
      if (cb.checked) {
        selected.add(cb.value);
      } else {
        selected.delete(cb.value);
      }
      saveSelection(selected);
      applyToPage();
      injectHiddenInputs();
    });
  });

  const form = activityList.closest("form");
  if (form) {
    form.addEventListener("submit", () => {
      injectHiddenInputs();
      try { sessionStorage.removeItem(STORAGE_KEY); } catch (_) {}
    });
  }

  applyToPage();
  injectHiddenInputs();
})();
