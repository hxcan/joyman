    private void setupProjectSpinner() {
        projectList = new ArrayList<>();
        projectSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        projectSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProjects.setAdapter(projectSpinnerAdapter);
        
        spinnerProjects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedProject = null;
                    loadAllTasks();
                } else {
                    // 修复：直接使用 position，因为 projectList[0] = null, projectList[1] = 第一个项目
                    selectedProject = projectList.get(position);
                    if (selectedProject != null) {
                        loadTasksByProject(selectedProject.getId());
                    }
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }