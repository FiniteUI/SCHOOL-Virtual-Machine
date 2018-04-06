/*
Author:	Richard Seddon
Date:	4-26-17
Compile: javac StackMachineRWS.java
Run:	java StackMachineRWS input.bin
							|input.dat
							|input.txt
							|input.asm (as long as there are compatible instructions)
Purpose: This program is emulates a stack machine with operators listed below.
*/
import java.util.Stack;
import java.util.Scanner;
import java.io.*;
public class StackMachineRWS {

	//opcodes
	static final int HALT     = 0,		//halt execution
					 PUSH     = 1,		//push a value on the stack
					 RVALUE   = 2,		//push the contents of an address on the stack
					 LVALUE   = 3,		//push an address on the stack
					 POP      = 4,		//remove the top item from the stack
					 STO      = 5,		//rvalue on top placed in lvalue below and both popped
					 COPY     = 6,		//push a copy of the top stack item
					 ADD      = 7,		//pop 2 items, add em, push the result
					 SUB      = 8,		//pop 2 items, subtract em, push the result
					 MPY      = 9,		//pop 2 items, multiply em, push the result
					 DIV      = 10,		//pop 2 items, divide em, push the result
					 MOD      = 11,		//pop 2 items, mod em, push the result
					 NEG      = 12,		//negate the top item on the stack
					 NOT      = 13,		//invert the bits of the top item on the stack
					 OR       = 14,		//pop 2 items, or em, push the result
					 AND      = 15,		//pop 2 items, and em, push the result
					 EQ       =	16,		//pop 2 items, if they're equal push 1, else push 0
					 NE       =	17,		//pop 2 items, if they're equal push 0, else push 1
					 GT       = 18,		//pop 2 items, if the second is greater than the first, push 1, else push 0
					 GE       = 19,		//pop 2 items, if the second is greater than or equal to the first, push 1, else push 0
					 LT       = 20,		//pop 2 items, if the second is less than the first, push 1, else push 0
					 LE       = 21,		//pop 2 items, if the second is less than or equal to the first, push 1, else push 0
					 LABEL	  =	22,		//Target for jumps. Mainly for the assembler, doesn't actually do anything on the machine
					 GOTO	  =	23,		//pc = n
					 GOFALSE  =	24,		//pop an item, if it's 0, pc = n, else do nothing
					 GOTRUE	  =	25,		//pop an item, if it's 1, pc = n, else do nothing
					 PRINT	  =	26,		//pop an item and print it
					 READ	  =	27,		//read an int from the keyboard, push it
					 GOSUB	  =	28,		//push the current pc on the callstack, pc = n
					 RET	  =	29,		//pc = pop the callstack
					 ORB      = 30,		//pop 2, bitwise or em, push the result
					 ANDB     = 31,		//pop 2, bitwise and em, push the result
					 XORB     = 32,		//pop 2, bitwise XOR em, push the result
					 SHL	  =	33,		//pop 1, shift it left 1 bit, push the result
					 SHR	  =	34,		//pop 1, shift it right 1 bit, push the result
					 SAR	  =	35,		//pop 1, shift it right 1 bit (maintain the top bit), push the result
					 MAX_CODE = 65536,	//The size of code memory
					 MAX_DATA = 65536;	//The size of data memory

	int[] codeMem,		//holds code to be executed
		  dataMem; 	    //holds data in memory to use

	int pc, 		//program counter: holds address of next instruction
		IR, 		//Instruction register: holds next instruction to be executed
		sp,			//stack pointer: containts the address in dataMem for the top of the stack
		stackLimit, //highest address of currently used memory; for checking stack overflow
		operand,	//holds current operand
		opcode,		//holds current opcode
		r1,			//for use in computations (register 1)
		r2;			//for use in computations (register 2)


	Stack<Integer> cStack;	//call stack

	boolean run;		//controls if whole machine runs or not

	Scanner sc;

	public StackMachineRWS() {//Lots of initializations

		codeMem = new int[MAX_CODE];
		dataMem = new int[MAX_DATA];
		pc = 0;
		run = true;
		sp = MAX_DATA;
		stackLimit = 0;
		cStack = new Stack<Integer>();
		sc = new Scanner(System.in);

	}//end of constructor

	public static void main(String[] args)throws IOException {
		StackMachineRWS SM = new StackMachineRWS();
		SM.getCode(args);
		System.out.println("Beginning execution...");
		SM.go();
		System.out.println("Done.");
	}//end of main

	public void fetch() {
		IR = codeMem[pc++];
	}//end fetch

	public void push(int pushVal) {
		sp = sp - 1;
		if (sp > stackLimit) {
			if (sp - MAX_DATA < MAX_DATA) {
				dataMem[sp] = pushVal;
			}//end inner if
			else {
				//Memory completely full with stack, no option but halting execution
				System.err.println("Stack overflow: stack has completely consumed memory");
				System.exit(0); //for now
			}//end inner else
		}//end outer if
		else {
			System.err.println("Stack Overflow: stack has reached occupied area of memory");
			System.exit(0); //for now, or run = false;
		}//end outer else
	}//end of push

	public int pop() {
		sp = sp + 1;
		if (sp <= MAX_DATA) {
			return dataMem[sp - 1];
		}//end if
		else {
			sp = sp -1;
			//maybe set a flag here to let user know of underflow, but it doesn't need to halt the program
			return 0;
		}//end else
	}//end of pop

	/*
	Note:
	For all my binary operators where order matters, I did them in an order as if it was postfix, because I believe its
	meant to work that way, and my compiler generates postfix the commands in a postfix order (I think).
	For example (6 - 5 = 1), in postfix (6 5 - = 1) so operands are used in the order they are written (the same as in infix).
	So this machine works the same way, operands are used in the order they are entered, which is opposite the order they
	are popped. That is why subtraction is r1 = r2 - r1 and not r1 = r1 - r2
	*/
	public void execute() {
		opcode = IR >>> 16;
		switch(opcode) {
			case HALT://0
				run = false;
				break;
			case PUSH://1
				operand = IR & 0xFFFF;
				push(operand);
				break;
			case RVALUE://2
				operand = IR & 0xFFFF;
				push(dataMem[operand]);
				break;
			case LVALUE://3
				operand = IR & 0xFFFF;
				push(operand);
				break;
			case POP://4
				pop();
				break;
			case STO://5
				r1 = pop();
				r2 = pop();
				dataMem[r2] = r1;
				stackLimitCheck(r2);
				break;
			case COPY://6
				//just to maintain the sanctity of the stack, I won't access it directly
				r1 = pop();
				push(r1);
				push(r1);
				break;
			case ADD://7
				r1 = pop();
				r2 = pop();
				r1 = r2 + r1;
				push(r1);
				break;
			case SUB://8
				r1 = pop();
				r2 = pop();
				r1 = r2 - r1;
				push(r1);
				break;
			case MPY://9
				r1 = pop();
				r2 = pop();
				r1 = r2 * r1;
				push(r1);
				break;
			case DIV://10
				r1 = pop();
				r2 = pop();
				r1 = r2 / r1;
				push(r1);
				break;
			case MOD://11
				r1 = pop();
				r2 = pop();
				r1 = r2 % r1;
				push(r1);
				break;
			case NEG://12
				r1 = pop();
				r1 = -r1;
				push(r1);
				break;
			case NOT://13
				r1 = pop();
				r1 = ~r1;
				push(r1);
				break;
			case OR://14
				r1 = pop();
				r2 = pop();
				if ((r1 == 0) && (r2 == 0))
					push(0);
				else
					push(1);
				break;
			case AND://15
				r1 = pop();
				r2 = pop();
				if ((r1 != 0) && (r2 != 0))
					push(1);
				else
					push(0);
				break;
			case EQ://16
				r1 = pop();
				r2 = pop();
				if (r1 == r2)
					push(1);
				else
					push(0);
				break;
			case NE://17
				r1 = pop();
				r2 = pop();
				if (r1 == r2)
					push(0);
				else
					push(1);
				break;
			case GT://18
				r1 = pop();
				r2 = pop();
				if (r2 > r1)
					push(1);
				else
					push(0);
				break;
			case GE://19
				r1 = pop();
				r2 = pop();
				if (r2 >= r1)
					push(1);
				else
					push(0);
				break;
			case LT://20
				r1 = pop();
				r2 = pop();
				if (r2 < r1)
					push(1);
				else
					push(0);
				break;
			case LE://21
				r1 = pop();
				r2 = pop();
				if (r2 <= r1)
					push(1);
				else
					push(0);
				break;
			case LABEL://22
				/*
				Destination for jumps. The assembler uses the address of this statement in place of the associated label
				for jump statement operands
				*/
				break;
			case GOTO://23
				operand = IR & 0xFFFF;
				pc = operand;
				/*
				If the assembler uses the address of the label statement itself, this could also be pc = operand + 1;
				However if the assembler uses the address of the next statement, only pc = operand would work.
				pc = operand works both ways, just in the first case it will execute the label statement unneccesarily.
				This holds for the other jump statements as well.
				*/
				break;
			case GOFALSE://24
				operand = IR & 0xFFFF;
				r1 = pop();
				if (r1 == 0)
					pc = operand;
				break;
			case GOTRUE://25
				operand = IR & 0xFFFF;
				r1 = pop();
				if (r1 != 0)	//if r1 == 1 should work here also, since all my comparisons push either 1 or 0, but this is safer
					pc = operand;
				break;
			case PRINT://26
				r1 = pop();
				System.out.println(r1);
				break;
			case READ://27
				r1 = sc.nextInt();
				push(r1);
				break;
			case GOSUB://28
				operand = IR & 0xFFFF;
				cStack.push(pc);
				pc = operand;
				break;
			case RET://29
				pc = cStack.pop();
				break;
			case ORB://30
				r1 = pop();
				r2 = pop();
				r1 = r1 | r2;
				push(r1);
				break;
			case ANDB://31
				r1 = pop();
				r2 = pop();
				r1 = r1 & r2;
				push(r1);
				break;
			case XORB://32
				r1 = pop();
				r2 = pop();
				r1 = r1 ^ r2;
				push(r1);
				break;
			case SHL://33
				r1 = pop();
				r1 = r1 << 1;
				push(r1);
				break;
			case SHR://34
				r1 = pop();
				r1 = r1 >>> 1;
				push(r1);
				break;
			case SAR://36
				r1 = pop();
				r1 = r1 >> 1;
				push(r1);
				break;
			default:
				System.err.println("Unrecognized opcode");
				System.exit(opcode);
		}//end of switch
		//I could clear r1 and r2 here, but it shouldn't be neccesary. None of these operations use them without setting them first.
	}//end execute

	public void stackLimitCheck(int x) {//makes sure the stack doesn't cover up used memory
		if (x > stackLimit)
			stackLimit = x;
	}//end of stackLimitCheck

	public void go() {//runs the machine
		while (run) {
			fetch();
			execute();
		}//end while
	}//end start

	public void getCode(String[] a)throws IOException {
		if (a.length < 1) {
			System.err.println("Error: Program input file required");
			System.exit(0);
		}//end if
		else if (a.length > 1) {
			System.err.println("Error: Too many arguments");
			System.exit(0);
		}//end else if
		FileInputStream fStream = new FileInputStream(a[0]);
		DataInputStream inFile = new DataInputStream(fStream);
		boolean eof = false;
		while (!(eof)) {
			try 	{
				codeMem[pc++] = inFile.readInt();
			}//end try
			catch (EOFException e) {
				eof = true;
			}//end catch
		}//end of while
		pc = 0;
		inFile.close();
		fStream.close();
	}//end of getCode
}//end of class