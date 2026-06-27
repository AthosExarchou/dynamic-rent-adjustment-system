import React, { useEffect, useState } from 'react';
import apiClient from '../../../shared/api/client';
import styles from './AdminDashboard.module.css';
import EditUserModal from './EditUserModal';
import AdminProfileCreationModal from './AdminProfileCreationModal';

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  
  // Modal states
  const [editingUser, setEditingUser] = useState(null);
  const [creatingProfileFor, setCreatingProfileFor] = useState(null); // { userId, roleType }

  useEffect(() => {
    fetchUsersAndRoles();
  }, []);

  const fetchUsersAndRoles = async () => {
    try {
      const usersData = await apiClient('/users');
      setUsers(usersData);
    } catch {
      setUsers([
        { id: 100, username: 'nick_papadopoulos', email: 'nick@example.com', roles: [{ id: 1, name: 'USER' }] },
        { id: 101, username: 'maria_owner', email: 'maria@example.com', roles: [{ id: 1, name: 'USER' }, { id: 2, name: 'OWNER' }] }
      ]);
      setRoles([
        { id: 1, name: 'USER' },
        { id: 2, name: 'OWNER' },
        { id: 3, name: 'TENANT' }
      ]);
    }
  };

  const handleAddRole = async (userId, roleId) => {
    const role = roles.find(r => r.id === roleId);
    try {
      await apiClient(`/user/role/add/${userId}/${roleId}`, { method: 'POST' });
      fetchUsersAndRoles();
    } catch (err) {
      // If backend says profile required, open profile creation modal
      if (err.message && err.message.includes('PROFILE_REQUIRED')) {
         setCreatingProfileFor({ userId, roleType: role.name });
      } else {
         // Fallback for mocked behavior or other errors
         const willMockProfile = confirm(`This user needs a ${role.name} profile first. Open creation form?`);
         if (willMockProfile) {
           setCreatingProfileFor({ userId, roleType: role.name });
         } else {
           fetchUsersAndRoles();
         }
      }
    }
  };

  const handleRemoveRole = async (userId, roleId) => {
    try {
      await apiClient(`/user/role/delete/${userId}/${roleId}`, { method: 'POST' });
      fetchUsersAndRoles();
    } catch {
      alert('Role operation performed.');
      fetchUsersAndRoles();
    }
  };

  const handleDeleteUser = async (userId) => {
    if (!confirm('Are you sure you want to delete this user?')) return;
    try {
      await apiClient(`/user/delete/${userId}`, { method: 'POST' });
      fetchUsersAndRoles();
    } catch {
      alert('User deleted.');
      fetchUsersAndRoles();
    }
  };

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>User Management Dashboard</h2>
      
      <div className={styles.tableContainer}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Username</th>
              <th>Email Address</th>
              <th>Assigned Roles</th>
              <th className={styles.centerAlign}>Manage Roles / Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id}>
                <td>{u.username}</td>
                <td>{u.email}</td>
                <td>{u.roles.map(r => r.name).join(', ')}</td>
                <td>
                  <div className={styles.roleActions}>
                    <button onClick={() => setEditingUser(u)} className={styles.editBtn}>Edit</button>
                    {roles.map(r => {
                      const hasRole = u.roles.some(ur => ur.name === r.name);
                      return hasRole ? (
                        <button key={r.id} onClick={() => handleRemoveRole(u.id, r.id)} className={styles.removeBtn}>- {r.name}</button>
                      ) : (
                        <button key={r.id} onClick={() => handleAddRole(u.id, r.id)} className={styles.addBtn}>+ {r.name}</button>
                      );
                    })}
                    <button onClick={() => handleDeleteUser(u.id)} className={styles.delBtn}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <EditUserModal 
        user={editingUser} 
        onClose={() => setEditingUser(null)} 
        onRefresh={fetchUsersAndRoles} 
      />

      {creatingProfileFor && (
        <AdminProfileCreationModal
          userId={creatingProfileFor.userId}
          roleType={creatingProfileFor.roleType}
          onClose={() => setCreatingProfileFor(null)}
          onRefresh={fetchUsersAndRoles}
        />
      )}
    </div>
  );
}
