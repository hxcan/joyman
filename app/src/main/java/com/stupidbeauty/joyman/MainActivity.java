    /**
     * 显示添加任务对话框
     */
    private void showAddTaskDialog() {
        // 创建垂直布局容器
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        // 任务标题输入框
        EditText editTextTitle = new EditText(this);
        editTextTitle.setHint(R.string.new_task_hint);
        editTextTitle.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(editTextTitle);
        
        // 项目选择 Spinner
        Spinner spinnerProject = new Spinner(this);
        spinnerProject.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        // 准备项目列表数据
        List<String> projectNames = new ArrayList<>();
        List<Long> projectIds = new ArrayList<>();
        
        // 添加"无项目"选项
        projectNames.add(getString(R.string.no_project));
        projectIds.add(null);
        
        // 添加所有项目
        if (projectList != null) {
            for (int i = 1; i < projectList.size(); i++) {
                Project project = projectList.get(i);
                if (project != null) {
                    projectNames.add(project.getIconDisplay() + " " + project.getName());
                    projectIds.add(project.getId());
                }
            }
        }
        
        // 设置 Spinner 适配器
        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            projectNames
        );
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProject.setAdapter(projectAdapter);
        
        // 将 Spinner 添加到布局
        layout.addView(spinnerProject);
        
        // 创建对话框
        new AlertDialog.Builder(this)
            .setTitle(R.string.add_task)
            .setView(layout)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                String title = editTextTitle.getText().toString().trim();
                if (!title.isEmpty()) {
                    // 获取选中的项目 ID
                    int selectedPosition = spinnerProject.getSelectedItemPosition();
                    Long projectId = null;
                    if (selectedPosition > 0 && selectedPosition < projectIds.size()) {
                        projectId = projectIds.get(selectedPosition);
                    }
                    
                    // 创建任务
                    long taskId = taskViewModel.createTask(title);
                    
                    // 如果选择了项目，更新任务的项目 ID
                    if (projectId != null) {
                        Task task = new Task(taskId, title);
                        task.setProjectId(projectId);
                        taskViewModel.update(task);
                    }
                    
                    Toast.makeText(this, R.string.task_created, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.task_empty_title, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }