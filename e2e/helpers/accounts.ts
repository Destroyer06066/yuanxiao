export interface TestAccount {
  username: string;
  password: string;
  role: 'OP_ADMIN' | 'SCHOOL_ADMIN' | 'SCHOOL_STAFF';
}

export const ACCOUNTS: Record<string, TestAccount> = {
  op_admin: {
    username: 'op_admin',
    password: 'OpAdmin@2026',
    role: 'OP_ADMIN',
  },
  school_admin: {
    username: 'testuser001',
    password: 'TestPass@123',
    role: 'SCHOOL_ADMIN',
  },
  school_staff: {
    username: 'test_staff_001',
    password: 'TestPass@123',
    role: 'SCHOOL_STAFF',
  },
};

export const API_BASE = 'http://localhost:8080/api/v1';
