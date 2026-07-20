(() => {
  const selectAll = document.getElementById("select-all-activities");
  if (!selectAll) return;

  const activityCheckboxes = Array.from(
    document.querySelectorAll('.activity-item input[name="activityIds"]')
  );

  const updateSelectAll = () => {
    const checkedCount = activityCheckboxes.filter((checkbox) => checkbox.checked).length;
    selectAll.checked = checkedCount === activityCheckboxes.length;
    selectAll.indeterminate = checkedCount > 0 && checkedCount < activityCheckboxes.length;
  };

  selectAll.addEventListener("change", () => {
    activityCheckboxes.forEach((checkbox) => {
      checkbox.checked = selectAll.checked;
    });
    selectAll.indeterminate = false;
  });

  activityCheckboxes.forEach((checkbox) => {
    checkbox.addEventListener("change", updateSelectAll);
  });

  updateSelectAll();
})();
