    private void showParentTaskSelectorDialog() {
        if (task == null) return;
        
        // 在后台线程查询数据库，避免阻塞主线程
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            List<Task> availableTasks = new ArrayList<>();
            List<Task> allTasks = taskViewModel.getTaskDao().getAllTasks();
            
            if (allTasks != null) {
                Long currentProjectId = task.getProjectId();
                for (Task t : allTasks) {
                    if (t.getId() != taskId) {
                        boolean isOpen = (t.getStatus() == Task.STATUS_NEW || t.getStatus() == Task.STATUS_IN_PROGRESS);
                        boolean sameProject = (currentProjectId == null && t.getProjectId() == null) ||
                                             (currentProjectId != null && currentProjectId.equals(t.getProjectId())) ||
                                             (currentProjectId == null);
                        if (isOpen && sameProject) availableTasks.add(t);
                    }
                }
            }
            
            // 切换回主线程更新 UI
            runOnUiThread(() -> {
                if (availableTasks.isEmpty()) {
                    Toast.makeText(this, "没有可选的上级任务", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_parent_task_selector, null);
                
                EditText editSearch = dialogView.findViewById(R.id.edit_parent_task_search);
                RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_view_parent_tasks);
                TextView textNoTasks = dialogView.findViewById(R.id.text_no_parent_tasks);
                
                List<Task> filteredTasks = new ArrayList<>(availableTasks);
                ParentTaskAdapter adapter = new ParentTaskAdapter(filteredTasks, taskId);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);
                
                editSearch.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String keyword = s.toString().toLowerCase().trim();
                        filteredTasks.clear();
                        if (keyword.isEmpty()) {
                            filteredTasks.addAll(availableTasks);
                        } else {
                            for (Task t : availableTasks) {
                                if (String.valueOf(t.getId()).contains(keyword) || 
                                    (t.getTitle() != null && t.getTitle().toLowerCase().contains(keyword))) {
                                    filteredTasks.add(t);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        recyclerView.setVisibility(filteredTasks.isEmpty() ? View.GONE : View.VISIBLE);
                        textNoTasks.setVisibility(filteredTasks.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
                
                builder.setTitle("选择上级任务")
                    .setView(dialogView)
                    .setPositiveButton("清除父任务", (dialog, which) -> {
                        pendingParentId = -1L;
                        updateSaveButtonState();
                        Toast.makeText(this, "已清除父任务", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null);
                
                AlertDialog dialog = builder.create();
                dialog.show();
                
                adapter.setOnTaskClickListener(selectedTask -> {
                    pendingParentId = selectedTask.getId();
                    updateSaveButtonState();
                    Toast.makeText(this, "已选择：" + selectedTask.getTitle(), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            });
        });
    }